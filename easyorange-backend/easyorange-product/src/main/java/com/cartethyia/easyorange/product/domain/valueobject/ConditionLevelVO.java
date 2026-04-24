package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.product.enums.ConditionLevel;

public record ConditionLevelVO(ConditionLevel value) {
    public ConditionLevelVO {
        if (value == null) {
            throw new IllegalArgumentException("新旧程度不能为空");
        }
    }
}