package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record SellerId(Long value) {
    public SellerId {
        if (value != null) {
            BizRequire.positive(value, "卖家ID必须为正数");
        }
    }

    public static SellerId of(Long value) {
        return new SellerId(value);
    }
}