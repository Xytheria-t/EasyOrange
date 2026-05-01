package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import org.jetbrains.annotations.NotNull;

public record SellerId(Long value) {

    public static final SellerId EMPTY = new SellerId(null);

    public SellerId {
        if (value != null) {
            BizRequire.positive(value, "卖家ID必须为正数");
        }
    }

    public static SellerId of(Long value) {
        if (value == null) {
            return EMPTY;
        }
        return new SellerId(value);
    }

    @Override
    @NotNull
    public String toString() {
        return value != null ? value.toString() : "(未持久化)";
    }
}
