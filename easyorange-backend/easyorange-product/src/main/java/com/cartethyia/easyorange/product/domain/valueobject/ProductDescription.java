package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductDescription(String value) {
    public ProductDescription {
        if (value != null && !value.isBlank()) {
            BizRequire.between(value.length(), 0, 5000, "商品描述长度必须在 0-5000 个字符之间");
            value = value.trim();
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
}