package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductDescription(String value) {
    public ProductDescription {
        if (value != null && !value.isBlank()) {
            BizRequire.isTrue(value.length() <= 5000, "商品描述不能超过5000个字符");
            value = value.trim();
        } else {
            value = null;
        }
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }
}