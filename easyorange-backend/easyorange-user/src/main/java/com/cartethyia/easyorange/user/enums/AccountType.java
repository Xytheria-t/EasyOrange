package com.cartethyia.easyorange.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
    WECHAT("wechat", "微信登录账户"),
    WEB("web", "Web注册账户"),
    BOTH("both", "混合账户");

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