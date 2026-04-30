package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductId(Long value) {
    public ProductId {
        if (value != null) {
            BizRequire.positive(value, "商品ID必须为正数");
        }
    }

    public static ProductId of(Long value) {
        return new ProductId(value);
    }

    public boolean isPersisted() {
        return value != null;
    }
}