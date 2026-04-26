package com.cartethyia.easyorange.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex implements IEnum<String> {
    UNKNOWN("0", "未知"),
    MALE("1", "男"),
    FEMALE("2", "女");

    @EnumValue
    private final String code;

    private final String description;

    @Override
    public String getValue() {
        return code;
    }

    public static Sex fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            return UNKNOWN;
        }
        return values()[ordinal];
    }
}