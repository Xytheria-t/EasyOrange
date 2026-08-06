package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message")
public class MessageDO extends BaseDO {

    private String senderId;
    private String receiverId;
    private Integer type;
    private String title;
    private String content;
    private ReadStatus isRead;
    private LocalDateTime readTime;
    private String businessId;
    private String conversationId;
    private MessageStatus msgStatus;
    private LocalDateTime recalledAt;

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public Integer getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public ReadStatus getIsRead() {
        return isRead;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public MessageStatus getMsgStatus() {
        return msgStatus;
    }

    public LocalDateTime getRecalledAt() {
        return recalledAt;
    }
}
