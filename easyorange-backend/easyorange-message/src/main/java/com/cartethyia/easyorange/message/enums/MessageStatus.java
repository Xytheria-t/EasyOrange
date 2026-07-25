package com.cartethyia.easyorange.message.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举 —— 对应 {@code eo_message.msg_status} 列（VARCHAR(20) 语义串）。
 * <p>
 * 统一使用语义串 code（与列存值 SENT/DELIVERED/READ/RECALLED 对齐），
 * 不再混用 Integer/String。
 */
@Getter
@AllArgsConstructor
public enum MessageStatus implements BaseCodeEnum {

    UNREAD("UNREAD", "未读"),
    READ("READ", "已读"),
    SENT("SENT", "已发送"),
    DELIVERED("DELIVERED", "已送达"),
    RECALLED("RECALLED", "已撤回");

    @JsonValue
    private final String code;
    private final String desc;

    public static MessageStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(MessageStatus.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
