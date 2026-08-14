package com.cartethyia.easyorange.user.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex {
    FEMALE("0", "女"),
    MALE("1", "男"),
    UNKNOWN("2", "未知");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    public static Sex fromCode(String code) {
        for (var sex : values()) {
            if (sex.code.equals(code)) {
                return sex;
            }
        }
        throw new IllegalArgumentException("Unknown Sex code: " + code);
    }
}
