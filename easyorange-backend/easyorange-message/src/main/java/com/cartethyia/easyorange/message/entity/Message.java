package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    private String msgStatus;
    private LocalDateTime recalledAt;

    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public Integer getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getIsRead() { return isRead; }
    public Long getBusinessId() { return businessId; }
    public Long getConversationId() { return conversationId; }
    public LocalDateTime getReadTime() { return readTime; }
    public String getMsgStatus() { return msgStatus; }
    public LocalDateTime getRecalledAt() { return recalledAt; }
}
