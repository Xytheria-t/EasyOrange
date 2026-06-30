package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record CategoryId(@JsonValue String value) {

    public static final CategoryId EMPTY = new CategoryId(null);

    public CategoryId {
        if (value != null) {
            BizRequire.requireTrue(!value.isBlank(), "分类ID不能为空");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    @JsonCreator
    public static CategoryId of(String value) {
        if (value == null) {
            return EMPTY;
        }
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return isPersisted() ? value : "(未持久化)";
    }
}
