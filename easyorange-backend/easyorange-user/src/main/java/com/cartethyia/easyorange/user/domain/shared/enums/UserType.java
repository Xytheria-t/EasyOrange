package com.cartethyia.easyorange.user.domain.shared.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType implements IEnum<String> {
    ADMIN("00", "管理员"),
    NORMAL("01", "普通用户");

    @EnumValue
    private final String code;

    private final String description;

    @Override
    public String getValue() {
        return code;
    }

    public static UserType fromCode(String code) {
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
