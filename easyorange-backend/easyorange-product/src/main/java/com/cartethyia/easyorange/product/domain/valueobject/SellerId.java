package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SellerId(@JsonValue Long value) {

    public static final SellerId EMPTY = new SellerId(null);

    public SellerId {
        if (value != null) {
            BizRequire.requireTrue(value > 0, "卖家ID必须为正数");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    @JsonCreator
    public static SellerId of(Long value) {
        if (value == null) {
            return EMPTY;
        }
        return new SellerId(value);
    }

    @Override
    public String toString() {
        return isPersisted() ? value.toString() : "(未持久化)";
    }
}
