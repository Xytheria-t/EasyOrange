package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConditionLevel implements BaseCodeEnum {
    NEW("1", "全新"),
    LIKE_NEW("2", "几乎全新"),
    GOOD("3", "轻微使用痕迹"),
    FAIR("4", "明显使用痕迹");

    @JsonValue
    private final String code;

    private final String desc;

    public static ConditionLevel fromCode(String code) {
        return BaseCodeEnum.fromCode(ConditionLevel.class, code);
    }
}
