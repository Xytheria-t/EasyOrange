package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 消息聚合根 —— 不可变 record。
 * <p>
 * 取舍：会话 ID 由聚合根按「排序双 ID」算出而非调用方传入——它是收发双方的纯函数，
 * 客户端可控的入参只会让同一房间被命名成两个值。
 * <p>
 * 不变量：只有接收者可标已读（已读幂等返回自身）；只有发送者可撤回；撤回须在 2 分钟内且未撤回过；
 * 标题与内容存原始文本，XSS 防护由渲染端承担，写入不转义（避免渲染时双转义）。
 * <p>
 * 边界：系统消息（senderId 为 null）无会话概念，conversationId 恒 null；
 * 老数据（列接入前写入的行）同样为 null，只影响按会话查询。
 */
public record Message(
        String id,
        String senderId,
        String receiverId,
        MessageType type,
        String title,
        String content,
        ReadStatus isRead,
        LocalDateTime readTime,
        String businessId,
        String conversationId,
        MessageStatus msgStatus,
        LocalDateTime recalledAt,
        LocalDateTime createTime) {

    // ── 工厂方法 ──

    /** 消息 ID 由应用层 {@code IdGenerator} 生成（{@code BaseDO.id} 为 {@code IdType.INPUT}，数据库不回填）。 */
    public static Message create(
            String id,
            String senderId,
            String receiverId,
            MessageType type,
            String title,
            String content,
            String businessId) {
        return new Message(
                id,
                senderId,
                receiverId,
                type,
                title,
                content,
                ReadStatus.UNREAD,
                null,
                businessId,
                conversationIdOf(senderId, receiverId),
                MessageStatus.SENT,
                null,
                LocalDateTime.now());
    }

    /** 消息 ID 由应用层 {@code IdGenerator} 生成（{@code BaseDO.id} 为 {@code IdType.INPUT}，数据库不回填）。 */
    public static Message createSystem(String id, String receiverId, String title, String content, String businessId) {
        return new Message(
                id,
                null,
                receiverId,
                MessageType.SYSTEM,
                title,
                content,
                ReadStatus.UNREAD,
                null,
                businessId,
                null,
                MessageStatus.SENT,
                null,
                LocalDateTime.now());
    }

    // ── 重建 ──

    public static Message fromRaw(
            String id,
            String senderId,
            String receiverId,
            MessageType type,
            String title,
            String content,
            ReadStatus isRead,
            LocalDateTime readTime,
            String businessId,
            String conversationId,
            MessageStatus msgStatus,
            LocalDateTime recalledAt,
            LocalDateTime createTime) {
        return new Message(
                id,
                senderId,
                receiverId,
                type,
                title,
                content,
                isRead,
                readTime,
                businessId,
                conversationId,
                msgStatus,
                recalledAt,
                createTime);
    }

    // ── 状态判定 ──

    public boolean isUnread() {
        return ReadStatus.UNREAD == this.isRead;
    }

    public boolean isOwnedBy(String userId) {
        return this.receiverId != null && this.receiverId.equals(userId);
    }

    public boolean isSender(String userId) {
        return this.senderId != null && this.senderId.equals(userId);
    }

    // ── 状态迁移 ──

    /** 幂等：已读直接返回自身；非接收者抛 {@link MessageDomainException#notOwner}。 */
    public Message read(String userId) {
        if (!isOwnedBy(userId)) {
            throw MessageDomainException.notOwner("只有接收者才能读取该消息");
        }
        if (ReadStatus.READ == this.isRead) {
            return this;
        }
        return new Message(
                this.id,
                this.senderId,
                this.receiverId,
                this.type,
                this.title,
                this.content,
                ReadStatus.READ,
                LocalDateTime.now(),
                this.businessId,
                this.conversationId,
                this.msgStatus,
                this.recalledAt,
                this.createTime);
    }

    /**
     * 撤回 —— 2 分钟窗口、已撤回不可再撤回；非发送者抛 {@link MessageDomainException#notOwner}。
     * <p>
     * 返回的事件自带本消息的 conversationId，订阅方据此定向广播。
     */
    public MessageRecallResult recall(String operatorId) {
        if (!isSender(operatorId)) {
            throw MessageDomainException.notOwner("不能撤回他人的消息");
        }
        if (MessageStatus.RECALLED == this.msgStatus) {
            throw MessageDomainException.of("消息已被撤回");
        }
        Duration elapsed = Duration.between(this.createTime, LocalDateTime.now());
        if (elapsed.toMinutes() >= 2) {
            throw MessageDomainException.of("消息已超过可撤回时间（2分钟）");
        }
        LocalDateTime now = LocalDateTime.now();
        Message updated = new Message(
                this.id,
                this.senderId,
                this.receiverId,
                this.type,
                this.title,
                this.content,
                this.isRead,
                this.readTime,
                this.businessId,
                this.conversationId,
                MessageStatus.RECALLED,
                now,
                this.createTime);
        return new MessageRecallResult(
                updated, new MessageRecalledEvent(UuidV7.generateId(), this.id, this.conversationId, operatorId, now));
    }

    /**
     * 判断用户是否为会话参与者 —— STOMP 订阅与 typing 帧的归属校验唯一判据。
     * UUID 不含下划线，剥掉 {@code conv_} 前缀按 {@code _} 拆分即得双参与者；
     * 格式不合法一律 false（fail-closed），不依赖 DB 查询即可裁决。
     */
    public static boolean isConversationParticipant(String conversationId, String userId) {
        if (conversationId == null || userId == null || !conversationId.startsWith("conv_")) {
            return false;
        }
        String[] parts = conversationId.substring("conv_".length()).split("_");
        return parts.length == 2 && (userId.equals(parts[0]) || userId.equals(parts[1]));
    }

    // ── 内部规则 ──

    /** 会话 ID：排序双 ID {@code conv_{min}_{max}}，保证 A→B 与 B→A 一致。 */
    private static String conversationIdOf(String senderId, String receiverId) {
        if (senderId == null || receiverId == null) {
            return null;
        }
        return senderId.compareTo(receiverId) < 0
                ? "conv_" + senderId + "_" + receiverId
                : "conv_" + receiverId + "_" + senderId;
    }

    // ── 返回结果 ──

    public record MessageRecallResult(Message aggregate, MessageRecalledEvent event) {}
}
