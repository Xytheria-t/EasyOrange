package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message_subscription")
public class MessageSubscription extends BaseDO {

    private Long userId;
    private String messageType;
    private String pushChannel;
    private Boolean enabled;

    public static MessageSubscription create(Long userId, String messageType, String pushChannel, Boolean enabled) {
        MessageSubscription subscription = new MessageSubscription();
        subscription.userId = userId;
        subscription.messageType = messageType;
        subscription.pushChannel = pushChannel;
        subscription.enabled = enabled;
        return subscription;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    public Long getUserId() { return userId; }
    public String getMessageType() { return messageType; }
    public String getPushChannel() { return pushChannel; }
    public Boolean getEnabled() { return enabled; }
}
