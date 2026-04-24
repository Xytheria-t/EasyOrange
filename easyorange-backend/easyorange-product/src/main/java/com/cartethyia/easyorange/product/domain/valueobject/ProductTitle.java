package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductTitle(String value) {
    public ProductTitle {
        BizRequire.notBlank(value, "商品名称不能为空");
        BizRequire.isTrue(value.length() <= 200, "商品名称不能超过200个字符");
        value = value.trim();
    }

    public String trimmed() {
        return value.trim();
    }
}