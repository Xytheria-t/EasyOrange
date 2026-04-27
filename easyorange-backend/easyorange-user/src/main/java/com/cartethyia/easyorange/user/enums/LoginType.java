package com.cartethyia.easyorange.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginType implements IEnum<String> {

    USERNAME("username", "用户名登录"),
    EMAIL("email", "邮箱登录"),
    PHONE("phone", "手机号登录");

    @EnumValue
    private final String type;

    private final String description;

    @Override
    public String getValue() {
        return type;
    }
}
