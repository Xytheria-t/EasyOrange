package com.cartethyia.easyorange.user.domain.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DelFlag {
    NORMAL("0", "未删除"),
    DELETED("2", "已删除");

    private final String code;
    private final String description;

    public static DelFlag fromCode(String code) {
        for (DelFlag flag : values()) {
            if (flag.code.equals(code)) {
                return flag;
            }
        }
        return null;
    }
}
