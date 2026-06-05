package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
import java.util.Arrays;

public enum ConditionLevel {

    NEW(1, "全新"),
    LIKE_NEW(2, "几乎全新"),
    GOOD(3, "轻微使用痕迹"),
    FAIR(4, "明显使用痕迹");

    private final Integer code;
    private final String desc;

    ConditionLevel(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * Resolves the enum value from its integer code.
     *
     * @param code the integer code (may be {@code null})
     * @return the matching enum value, or {@code null} if code is null or not recognized
     */
    @Nullable
    public static ConditionLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(level -> level.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static String getDescByCode(Integer code) {
        ConditionLevel level = fromCode(code);
        return level != null ? level.getDesc() : "未知";
    }
}
