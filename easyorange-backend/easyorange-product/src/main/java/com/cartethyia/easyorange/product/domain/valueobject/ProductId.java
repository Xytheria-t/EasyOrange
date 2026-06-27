package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ProductId(@JsonValue Long value) {

    public static final ProductId EMPTY = new ProductId(null);

    public ProductId {
        if (value != null) {
            BizRequire.requireTrue(value > 0, "资产ID必须为正数");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    @JsonCreator
    public static ProductId of(Long value) {
        if (value == null) {
            return EMPTY;
        }
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return isPersisted() ? value.toString() : "(未持久化)";
    }
}
