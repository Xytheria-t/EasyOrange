package com.cartethyia.easyorange.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClientType {
    WEB("web", "网页端");

    @JsonValue
    private final String code;

    private final String description;

    public static ClientType fromCode(String code) {
        for (var type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ClientType code: " + code);
    }
}