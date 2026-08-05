package com.cartethyia.easyorange.message.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum MessageType implements BaseCodeEnum {

    SYSTEM("1", "系统通知"),
    CHAT("2", "聊天消息"),
    ORDER("3", "订单消息"),
    PAYMENT("4", "支付消息"),
    ACTIVITY("5", "活动通知");

    @JsonValue
    private final String code;
    private final String desc;

    public static MessageType fromCode(String code) {
        return BaseCodeEnum.fromCode(MessageType.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知类型";
        }
    }
}
