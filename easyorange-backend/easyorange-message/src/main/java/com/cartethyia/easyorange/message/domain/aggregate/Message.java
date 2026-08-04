package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageReadEvent;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.UnauthorizedOperationException;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.enums.ReadStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 消息聚合根 —— 不可变对象
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>只有接收者可以标记已读、删除消息</li>
 *   <li>只有发送者可以撤回消息</li>
 *   <li>撤回必须在 2 分钟内完成</li>
 *   <li>已撤回的消息不能再次撤回</li>
 *   <li>标题和内容必须经过 XSS 转义</li>
 * </ul>
 */
public class Message {

    private final String id;
    private final String senderId;
    private final String receiverId;
    private final Integer type;
    private final String title;
    private final String content;
    private final ReadStatus isRead;
    private final LocalDateTime readTime;
    private final String businessId;
    private final MessageStatus msgStatus;
    private final LocalDateTime recalledAt;
    private final LocalDateTime createTime;

    private Message(String id, String senderId, String receiverId, Integer type,
                             String title, String content, ReadStatus isRead, LocalDateTime readTime,
                             String businessId, MessageStatus msgStatus, LocalDateTime recalledAt,
                             LocalDateTime createTime) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.isRead = isRead;
        this.readTime = readTime;
        this.businessId = businessId;
        this.msgStatus = msgStatus;
        this.recalledAt = recalledAt;
        this.createTime = createTime;
    }

    // ==================== Getters ====================

    public String id() { return id; }
    public String senderId() { return senderId; }
    public String receiverId() { return receiverId; }
    public Integer type() { return type; }
    public String title() { return title; }
    public String content() { return content; }
    public ReadStatus isRead() { return isRead; }
    public LocalDateTime readTime() { return readTime; }
    public String businessId() { return businessId; }
    public MessageStatus msgStatus() { return msgStatus; }
    public LocalDateTime recalledAt() { return recalledAt; }
    public LocalDateTime createTime() { return createTime; }

    // ==================== Factory ====================

    /**
     * 创建普通消息
     */
    public static MessageCreateResult create(String senderId, String receiverId, Integer type,
                                              String title, String content, String businessId) {
        Message aggregate = new Message(
                null, senderId, receiverId, type,
                escapeHtml(title), escapeHtml(content),
                ReadStatus.UNREAD, null,
                businessId, MessageStatus.SENT, null, null
        );
        return new MessageCreateResult(aggregate, new MessageSentEvent(null, senderId, receiverId, type));
    }

    /**
     * 创建系统消息
     */
    public static MessageCreateResult createSystem(String receiverId, String title,
                                                    String content, String businessId) {
        Message aggregate = new Message(
                null, null, receiverId, Integer.valueOf(MessageType.SYSTEM.getCode()),
                escapeHtml(title), escapeHtml(content),
                ReadStatus.UNREAD, null,
                businessId, null, null, null
        );
        return new MessageCreateResult(aggregate, new MessageSentEvent(null, null, receiverId, Integer.valueOf(MessageType.SYSTEM.getCode())));
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
    public static Message fromRaw(String id, String senderId, String receiverId, Integer type,
                                            String title, String content, ReadStatus isRead,
                                            LocalDateTime readTime, String businessId,
                                            MessageStatus msgStatus, LocalDateTime recalledAt,
                                            LocalDateTime createTime) {
        return new Message(id, senderId, receiverId, type,
                title, content, isRead, readTime,
                businessId, msgStatus, recalledAt, createTime);
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
     * 发送消息（返回领域事件）
     */
    public MessageSentEvent send() {
        return new MessageSentEvent(this.id, this.senderId, this.receiverId, this.type);
    }

    /**
     * 标记消息为已读
     *
     * @return 包含更新后聚合根和领域事件的结果；如果已读则返回 null
     * @throws UnauthorizedOperationException 如果 userId 不是接收者
     */
    public MessageReadResult read(String userId) {
        if (!this.receiverId.equals(userId)) {
            throw new UnauthorizedOperationException("Only receiver can read this message");
        }
        if (ReadStatus.READ == this.isRead) {
            return null;
        }
        Message updated = new Message(
                this.id, this.senderId, this.receiverId, this.type,
                this.title, this.content, ReadStatus.READ, LocalDateTime.now(),
                this.businessId, this.msgStatus, this.recalledAt, this.createTime
        );
        return new MessageReadResult(updated, new MessageReadEvent(this.id, userId));
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
                this.id, this.senderId, this.receiverId, this.type,
                this.title, this.content, this.isRead, this.readTime,
                this.businessId, MessageStatus.RECALLED, now, this.createTime
        );
        return new MessageRecallResult(updated, new MessageRecalledEvent(this.id, conversationId, operatorId, now));
    }

    /**
     * 删除消息
     *
     * @return 领域事件
     * @throws UnauthorizedOperationException 如果 userId 不是接收者
     */
    public MessageDeletedEvent delete(String userId) {
        if (!this.receiverId.equals(userId)) {
            throw new UnauthorizedOperationException("Not authorized to delete");
        }
        return new MessageDeletedEvent(this.id, userId);
    }

    // ==================== Internal ====================

    private static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    // ==================== Result Records ====================

    public record MessageCreateResult(Message aggregate, MessageSentEvent event) {}
    public record MessageReadResult(Message aggregate, MessageReadEvent event) {}
    public record MessageRecallResult(Message aggregate, MessageRecalledEvent event) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", type=" + type + ", msgStatus=" + msgStatus + "}";
    }
}
