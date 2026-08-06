package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductDescription(String value) {

    public static final int MAX_LENGTH = 5000;

    public ProductDescription {
        if (value != null && !value.isBlank()) {
            value = value.trim();
            BizRequire.requireTrue(value.length() <= MAX_LENGTH, "资产描述长度不能超过 " + MAX_LENGTH + " 个字符");
        } else {
            value = null;
        }
    }

    public static ProductDescription of(String value) {
        return new ProductDescription(value);
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }

    public boolean isPresent() {
        return value != null;
    }
}
