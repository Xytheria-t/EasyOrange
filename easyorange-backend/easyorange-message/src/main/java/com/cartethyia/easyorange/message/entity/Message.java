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

    private String senderId;
    private String receiverId;
    private Integer type;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime readTime;
    private String businessId;
    private String conversationId;
    private String msgStatus;
    private LocalDateTime recalledAt;

    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public Integer getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getIsRead() { return isRead; }
    public String getBusinessId() { return businessId; }
    public String getConversationId() { return conversationId; }
    public LocalDateTime getReadTime() { return readTime; }
    public String getMsgStatus() { return msgStatus; }
    public LocalDateTime getRecalledAt() { return recalledAt; }
}
