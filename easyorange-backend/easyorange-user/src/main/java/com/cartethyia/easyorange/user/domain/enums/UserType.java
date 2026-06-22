package com.cartethyia.easyorange.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType {
    ADMIN("00", "超级管理员"),
    NORMAL("01", "普通用户"),
    MANAGER("02", "管理员");

    @JsonValue
    private final String code;

    private final String description;

    public static UserType fromCode(String code) {
        for (var type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown UserType code: " + code);
    }

    /**
     * 判断是否为管理员类型（超级管理员或管理员）
     */
    public boolean isAdmin() {
        return this == ADMIN || this == MANAGER;
    }
}
