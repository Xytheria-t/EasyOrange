package com.cartethyia.easyorange.user.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex implements IEnum<String> {
    FEMALE("0", "女"),
    MALE("1", "男"),
    UNKNOWN("2", "未知");

    @EnumValue
    private final String code;

    private final String description;

    @Override
    public String getValue() {
        return code;
    }

    public static Sex fromCode(int code) {
        return fromCode(String.valueOf(code));
    }

    public static Sex fromCode(String code) {
        for (Sex sex : values()) {
            if (sex.code.equals(code)) {
                return sex;
            }
        }
        return UNKNOWN;
    }
}
