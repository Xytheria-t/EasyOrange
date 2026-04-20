package com.cartethyia.easyorange.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConditionLevel {

    NEW(1, "全新"),
    LIKE_NEW(2, "几乎全新"),
    GOOD(3, "轻微使用痕迹"),
    FAIR(4, "明显使用痕迹");

    private final Integer code;
    private final String desc;

    public static ConditionLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConditionLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
