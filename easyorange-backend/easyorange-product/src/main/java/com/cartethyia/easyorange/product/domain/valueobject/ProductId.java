package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import org.jetbrains.annotations.NotNull;

public record ProductId(Long value) {

    public static final ProductId EMPTY = new ProductId(null);

    public ProductId {
        if (value != null) {
            BizRequire.positive(value, "商品ID必须为正数");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    public static ProductId of(Long value) {
        if (value == null) {
            return EMPTY;
        }
        return new ProductId(value);
    }

    @Override
    @NotNull
    public String toString() {
        return isPersisted() ? value.toString() : "(未持久化)";
    }
}
