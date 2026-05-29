package com.cartethyia.easyorange.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginMethod {
    PASSWORD("password", "密码登录"),
    SMS("sms", "短信验证码登录");

    @JsonValue
    private final String code;

    private final String description;

    public static LoginMethod fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("LoginMethod code must not be null");
        }
        for (var method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown LoginMethod code: " + code);
    }
}