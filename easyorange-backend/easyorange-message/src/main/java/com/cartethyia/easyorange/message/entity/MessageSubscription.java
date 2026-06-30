package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message_subscription")
public class MessageSubscription extends BaseDO {

    private String userId;
    private String messageType;
    private String pushChannel;
    private Boolean enabled;

    public String getUserId() { return userId; }
    public String getMessageType() { return messageType; }
    public String getPushChannel() { return pushChannel; }
    public Boolean getEnabled() { return enabled; }
}
