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
@TableName("eo_offline_message")
public class OfflineMessage extends BaseDO {

    private String userId;
    private String messageId;
    private String pushChannel;
    private Integer pushStatus;
    private LocalDateTime pushTime;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime lastRetryTime;

    public String getUserId() { return userId; }
    public String getMessageId() { return messageId; }
    public String getPushChannel() { return pushChannel; }
    public Integer getPushStatus() { return pushStatus; }
    public LocalDateTime getPushTime() { return pushTime; }
    public Integer getRetryCount() { return retryCount; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public LocalDateTime getLastRetryTime() { return lastRetryTime; }
}
