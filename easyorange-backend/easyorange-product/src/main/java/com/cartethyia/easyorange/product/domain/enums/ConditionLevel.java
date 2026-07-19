package com.cartethyia.easyorange.product.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConditionLevel {

    NEW(1, "全新"),
    LIKE_NEW(2, "几乎全新"),
    GOOD(3, "轻微使用痕迹"),
    FAIR(4, "明显使用痕迹");

    @JsonValue
    private final int code;
    private final String desc;

    @Nullable
    @JsonCreator
    public static ConditionLevel fromCode(Integer code) {
        return switch (code) {
            case 1 -> NEW;
            case 2 -> LIKE_NEW;
            case 3 -> GOOD;
            case 4 -> FAIR;
            case null, default -> null;
        };
    }

    public static String getDescByCode(Integer code) {
        ConditionLevel level = fromCode(code);
        return level != null ? level.getDesc() : "未知";
    }
}
