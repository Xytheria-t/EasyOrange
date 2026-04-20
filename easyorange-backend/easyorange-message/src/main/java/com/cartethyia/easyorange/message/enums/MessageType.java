package com.cartethyia.easyorange.message.enums;

import com.cartethyia.easyorange.common.util.EnumUtils;
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

    SYSTEM(1, "系统通知"),
    CHAT(2, "聊天消息"),
    ORDER(3, "订单消息"),
    PAYMENT(4, "支付消息"),
    ACTIVITY(5, "活动通知");

    private final Integer code;
    private final String desc;

    public static MessageType fromCode(Integer code) {
        return EnumUtils.fromCodeSafe(code, values(), MessageType::getCode).orElse(null);
    }

    public static String getDescByCode(Integer code) {
        MessageType type = fromCode(code);
        return type != null ? type.getDesc() : "未知类型";
    }
}
