package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SellerId(@JsonValue String value) {

    public static final SellerId EMPTY = new SellerId(null);

    public SellerId {
        if (value != null) {
            BizRequire.requireTrue(!value.isBlank(), "资产方ID不能为空");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    @JsonCreator
    public static SellerId of(String value) {
        if (value == null) {
            return EMPTY;
        }
        return new SellerId(value);
    }

    @Override
    public String toString() {
        return isPersisted() ? value : "(未持久化)";
    }
}
