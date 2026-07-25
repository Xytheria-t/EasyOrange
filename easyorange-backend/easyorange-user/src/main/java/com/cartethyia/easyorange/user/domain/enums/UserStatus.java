package com.cartethyia.easyorange.user.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus implements BaseCodeEnum {
    NORMAL("0", "正常"),
    DISABLED("1", "禁用"),
    LOCKED("2", "锁定");

    @JsonValue
    private final String code;

    private final String description;

    public static UserStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(UserStatus.class, code);
    }
}