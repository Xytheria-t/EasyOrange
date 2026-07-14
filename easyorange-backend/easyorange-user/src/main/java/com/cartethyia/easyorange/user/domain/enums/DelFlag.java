package com.cartethyia.easyorange.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DelFlag {
    NORMAL("0", "未删除"),
    DELETED("1", "已删除");

    @JsonValue
    private final String code;

    private final String description;

    public static DelFlag fromCode(String code) {
        for (var flag : values()) {
            if (flag.code.equals(code)) {
                return flag;
            }
        }
        throw new IllegalArgumentException("Unknown DelFlag code: " + code);
    }
}