package com.cartethyia.easyorange.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginMethod implements IEnum<String> {

    PASSWORD("password", "密码登录"),
    SMS("sms", "短信验证码登录");

    @EnumValue
    private final String value;

    private final String description;

    @Override
    public String getValue() {
        return value;
    }

    public static LoginMethod fromValue(String value) {
        if (value == null) {
            return PASSWORD;
        }
        for (LoginMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("不支持的登录方式: " + value);
    }
}
