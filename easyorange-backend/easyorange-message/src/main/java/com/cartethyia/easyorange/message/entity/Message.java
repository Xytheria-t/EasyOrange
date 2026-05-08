package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageReadEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.domain.exception.UnauthorizedOperationException;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message")
public class Message extends BaseDO {

    private Long senderId;
    private Long receiverId;
    private Integer type;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime readTime;
    private Long businessId;
    private Long conversationId;

    public static Message create(Long senderId, Long receiverId, Integer type, String title, String content, Long businessId) {
        Message message = new Message();
        message.senderId = senderId;
        message.receiverId = receiverId;
        message.type = type;
        message.title = HtmlUtils.htmlEscape(title);
        message.content = HtmlUtils.htmlEscape(content);
        message.isRead = MessageStatus.UNREAD.getCode();
        message.businessId = businessId;
        return message;
    }

    public static Message createSystem(Long receiverId, String title, String content) {
        Message message = new Message();
        message.senderId = null;
        message.receiverId = receiverId;
        message.type = MessageType.SYSTEM.getCode();
        message.title = HtmlUtils.htmlEscape(title);
        message.content = HtmlUtils.htmlEscape(content);
        message.isRead = MessageStatus.UNREAD.getCode();
        return message;
    }

    public MessageSentEvent send() {
        return new MessageSentEvent(this.getId(), this.senderId, this.receiverId, this.type);
    }

    public MessageReadEvent read(Long userId) {
        if (!this.receiverId.equals(userId)) {
            throw new UnauthorizedOperationException("Only receiver can read this message");
        }
        if (MessageStatus.READ.getCode().equals(this.isRead)) {
            return null;
        }
        this.isRead = MessageStatus.READ.getCode();
        this.readTime = LocalDateTime.now();
        return new MessageReadEvent(this.getId(), userId);
    }

    public MessageDeletedEvent delete(Long userId) {
        if (!this.receiverId.equals(userId)) {
            throw new UnauthorizedOperationException("Not authorized to delete");
        }
        return new MessageDeletedEvent(this.getId(), userId);
    }

    public boolean isUnread() {
        return MessageStatus.UNREAD.getCode().equals(this.isRead);
    }

    public boolean isOwnedBy(Long userId) {
        return this.receiverId.equals(userId);
    }

    public void markAsRead() {
        this.isRead = MessageStatus.READ.getCode();
        this.readTime = LocalDateTime.now();
    }

    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public Integer getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getIsRead() { return isRead; }
    public Long getBusinessId() { return businessId; }
    public Long getConversationId() { return conversationId; }
}
