package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消息订阅实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message_subscription")
public class MessageSubscription extends BaseDO {

    /** 用户 ID */
    private Long userId;

    /** 消息类型：system, order, chat 等 */
    private String messageType;

    /** 推送渠道：websocket, email, sms 等 */
    private String pushChannel;

    /** 是否启用 */
    private Boolean enabled;
}
