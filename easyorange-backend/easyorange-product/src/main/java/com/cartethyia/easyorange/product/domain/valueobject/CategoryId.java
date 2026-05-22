package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record CategoryId(Long value) {

    public static final CategoryId EMPTY = new CategoryId(null);

    public CategoryId {
        if (value != null) {
            BizRequire.positive(value, "分类ID必须为正数");
        }
    }

    public boolean isPersisted() {
        return value != null;
    }

    public static CategoryId of(Long value) {
        if (value == null) {
            return EMPTY;
        }
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return isPersisted() ? value.toString() : "(未持久化)";
    }
}
