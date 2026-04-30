package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_offline_message")
public class OfflineMessage extends BaseDO {

    private Long userId;
    private Long messageId;
    private String pushChannel;
    private Integer pushStatus;
    private LocalDateTime pushTime;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime lastRetryTime;

    public static OfflineMessage create(Long userId, Long messageId, String pushChannel) {
        OfflineMessage msg = new OfflineMessage();
        msg.userId = userId;
        msg.messageId = messageId;
        msg.pushChannel = pushChannel;
        msg.pushStatus = MessageConstant.PUSH_STATUS_PENDING;
        msg.retryCount = MessageConstant.DEFAULT_RETRY_COUNT;
        msg.maxRetryCount = MessageConstant.DEFAULT_MAX_RETRY_COUNT;
        return msg;
    }

    public boolean isPending() {
        return this.pushStatus != null && this.pushStatus == MessageConstant.PUSH_STATUS_PENDING;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }

    public void markAsPushed() {
        this.pushStatus = MessageConstant.PUSH_STATUS_PUSHED;
        this.pushTime = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.pushStatus = MessageConstant.PUSH_STATUS_FAILED;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.lastRetryTime = LocalDateTime.now();
    }

    public Long getUserId() { return userId; }
    public Long getMessageId() { return messageId; }
    public String getPushChannel() { return pushChannel; }
    public Integer getPushStatus() { return pushStatus; }
    public LocalDateTime getPushTime() { return pushTime; }
    public Integer getRetryCount() { return retryCount; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public LocalDateTime getLastRetryTime() { return lastRetryTime; }
}
