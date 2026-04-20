package com.cartethyia.easyorange.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginType {

    USERNAME("username", "用户名登录"),
    EMAIL("email", "邮箱登录"),
    PHONE("phone", "手机号登录"),
    WECHAT("wechat", "微信登录");

    private final String type;
    private final String description;

    public static LoginType fromType(String type) {
        if (type == null) {
            return USERNAME;
        }
        for (LoginType loginType : values()) {
            if (loginType.type.equals(type)) {
                return loginType;
            }
        }
        return USERNAME;
    }
}
