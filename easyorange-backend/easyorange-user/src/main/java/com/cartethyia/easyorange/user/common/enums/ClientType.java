package com.cartethyia.easyorange.user.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClientType {
    WEB("web", "网页端");

    private final String code;
    private final String description;

    public static ClientType fromCode(String code) {
        if (code == null) {
            return WEB;
        }
        for (ClientType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return WEB;
    }
}
