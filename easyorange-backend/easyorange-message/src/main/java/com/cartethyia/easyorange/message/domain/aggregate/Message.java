package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.UnauthorizedOperationException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 消息聚合根 —— 不可变 record
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>只有接收者可以标记已读、删除消息</li>
 *   <li>只有发送者可以撤回消息</li>
 *   <li>撤回必须在 2 分钟内完成</li>
 *   <li>已撤回的消息不能再次撤回</li>
 *   <li>标题和内容以原始文本存储；XSS 防护由渲染端文本输出承担，写入不做转义（避免文本渲染时双转义）</li>
 * </ul>
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
        MessageStatus msgStatus,
        LocalDateTime recalledAt,
        LocalDateTime createTime) {

    // ==================== Factory ====================

    /**
     * 创建普通消息
     */
    public static Message create(
            String senderId, String receiverId, MessageType type, String title, String content, String businessId) {
        return new Message(
                null,
                senderId,
                receiverId,
                type,
                title,
                content,
                ReadStatus.UNREAD,
                null,
                businessId,
                MessageStatus.SENT,
                null,
                LocalDateTime.now());
    }

    /**
     * 创建系统消息
     */
    public static Message createSystem(String receiverId, String title, String content, String businessId) {
        return new Message(
                null,
                null,
                receiverId,
                MessageType.SYSTEM,
                title,
                content,
                ReadStatus.UNREAD,
                null,
                businessId,
                MessageStatus.SENT,
                null,
                LocalDateTime.now());
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
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
                msgStatus,
                recalledAt,
                createTime);
    }

    // ==================== Predicates ====================

    public boolean isUnread() {
        return ReadStatus.UNREAD == this.isRead;
    }

    public boolean isOwnedBy(String userId) {
        return this.receiverId != null && this.receiverId.equals(userId);
    }

    public boolean isSender(String userId) {
        return this.senderId != null && this.senderId.equals(userId);
    }

    // ==================== State Transitions ====================

    /**
     * 标记消息为已读（幂等：已读返回自身）。
     *
     * @return 已读后的消息；若本就已读则返回当前实例
     * @throws UnauthorizedOperationException 如果 userId 不是接收者
     */
    public Message read(String userId) {
        if (!isOwnedBy(userId)) {
            throw new UnauthorizedOperationException("Only receiver can read this message");
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
                this.msgStatus,
                this.recalledAt,
                this.createTime);
    }

    /**
     * 撤回消息
     *
     * @return 包含更新后聚合根和领域事件的结果
     * @throws UnauthorizedOperationException 如果 operatorId 不是发送者
     * @throws MessageDomainException         如果消息已撤回或超过 2 分钟
     */
    public MessageRecallResult recall(String operatorId, String conversationId) {
        if (!isSender(operatorId)) {
            throw new UnauthorizedOperationException("不能撤回他人的消息");
        }
        if (MessageStatus.RECALLED == this.msgStatus) {
            throw new MessageDomainException("消息已被撤回");
        }
        Duration elapsed = Duration.between(this.createTime, LocalDateTime.now());
        if (elapsed.toMinutes() >= 2) {
            throw new MessageDomainException("消息已超过可撤回时间（2分钟）");
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
                MessageStatus.RECALLED,
                now,
                this.createTime);
        return new MessageRecallResult(updated, new MessageRecalledEvent(this.id, conversationId, operatorId, now));
    }

    /**
     * 删除消息（仅校验接收者权限；删除动作由应用层执行）。
     *
     * @throws UnauthorizedOperationException 如果 userId 不是接收者
     */
    public void delete(String userId) {
        if (!isOwnedBy(userId)) {
            throw new UnauthorizedOperationException("Not authorized to delete");
        }
    }

    // ==================== Result Record ====================

    public record MessageRecallResult(Message aggregate, MessageRecalledEvent event) {}
}
