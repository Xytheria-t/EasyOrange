package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ProductId(@JsonValue String value) {

    public static final ProductId EMPTY = new ProductId(null);

    public ProductId {
        if (value != null) {
            BizRequire.requireTrue(!value.isBlank(), "资产ID不能为空");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    @JsonCreator
    public static ProductId of(String value) {
        if (value == null) {
            return EMPTY;
        }
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return isPersisted() ? value : "(未持久化)";
    }
}
