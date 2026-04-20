package com.cartethyia.easyorange.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    NORMAL("0", "正常"),
    DISABLED("1", "禁用"),
    LOCKED("2", "锁定");

    private final String code;
    private final String description;

    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
