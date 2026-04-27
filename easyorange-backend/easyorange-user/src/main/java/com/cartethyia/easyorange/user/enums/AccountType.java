package com.cartethyia.easyorange.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
    WEB("web", "Web注册账户");

    private final String code;
    private final String description;

    public static AccountType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AccountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
