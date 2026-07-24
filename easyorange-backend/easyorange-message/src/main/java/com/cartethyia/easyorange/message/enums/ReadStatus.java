package com.cartethyia.easyorange.message.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息读取状态枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum ReadStatus {

    UNREAD("0", "未读"),
    READ("1", "已读");

    @JsonValue
    private final String code;
    private final String desc;

    public static ReadStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("ReadStatus code must not be null");
        }
        for (var status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReadStatus code: " + code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
