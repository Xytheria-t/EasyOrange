package com.cartethyia.easyorange.user.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus implements BaseCodeEnum {
    NORMAL("NORMAL", "正常"),
    DISABLED("DISABLED", "禁用"),
    LOCKED("LOCKED", "锁定");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    public static UserStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(UserStatus.class, code);
    }
}
