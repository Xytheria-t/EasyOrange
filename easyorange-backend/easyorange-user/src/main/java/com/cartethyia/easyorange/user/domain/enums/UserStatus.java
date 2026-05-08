package com.cartethyia.easyorange.user.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus implements IEnum<String> {
    NORMAL("0", "正常"),
    DISABLED("1", "禁用"),
    LOCKED("2", "锁定");

    @EnumValue
    private final String code;

    private final String description;

    @Override
    public String getValue() {
        return code;
    }
}
