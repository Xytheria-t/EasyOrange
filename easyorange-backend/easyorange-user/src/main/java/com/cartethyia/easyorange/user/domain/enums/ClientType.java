package com.cartethyia.easyorange.user.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClientType implements BaseCodeEnum {
    WEB("web", "网页端");

    @JsonValue
    private final String code;

    private final String description;

    public static ClientType fromCode(String code) {
        return BaseCodeEnum.fromCode(ClientType.class, code);
    }
}
