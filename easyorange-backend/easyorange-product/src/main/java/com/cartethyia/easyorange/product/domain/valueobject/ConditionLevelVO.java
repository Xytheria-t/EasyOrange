package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;

public record ConditionLevelVO(ConditionLevel value) {
    public ConditionLevelVO {
        if (value == null) {
            throw new IllegalArgumentException("新旧程度不能为空");
        }
    }

    public static ConditionLevelVO of(Integer code) {
        return new ConditionLevelVO(ConditionLevel.fromCode(code));
    }

    public static ConditionLevelVO of(ConditionLevel level) {
        return new ConditionLevelVO(level);
    }

    public Integer code() {
        return value != null ? value.getCode() : null;
    }

    public String desc() {
        return value != null ? value.getDesc() : null;
    }
}