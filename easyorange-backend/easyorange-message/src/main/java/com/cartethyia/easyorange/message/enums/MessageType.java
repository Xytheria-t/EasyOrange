package com.cartethyia.easyorange.message.enums;

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
public enum MessageType {

    SYSTEM("1", "系统通知"),
    CHAT("2", "聊天消息"),
    ORDER("3", "订单消息"),
    PAYMENT("4", "支付消息"),
    ACTIVITY("5", "活动通知");

    @JsonValue
    private final String code;
    private final String desc;

    public static MessageType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("MessageType code must not be null");
        }
        for (var type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MessageType code: " + code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知类型";
        }
    }
}
