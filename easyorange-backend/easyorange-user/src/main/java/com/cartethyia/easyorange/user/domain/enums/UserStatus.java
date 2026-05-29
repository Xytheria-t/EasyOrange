package com.cartethyia.easyorange.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    NORMAL("0", "正常"),
    DISABLED("1", "禁用"),
    LOCKED("2", "锁定");

    @JsonValue
    private final String code;

    private final String description;

    public static UserStatus fromCode(String code) {
        for (var status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }
}