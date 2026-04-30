package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record CategoryId(Long value) {
    public CategoryId {
        if (value != null) {
            BizRequire.positive(value, "分类ID必须为正数");
        }
    }

    public static CategoryId of(Long value) {
        return new CategoryId(value);
    }
}