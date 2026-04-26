package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductTitle(String value) {
    public ProductTitle {
        BizRequire.notBlank(value, "商品名称不能为空");
        BizRequire.between(value.length(), 1, 200, "商品名称长度必须在 1-200 个字符之间");
        value = value.trim();
    }

    public String trimmed() {
        return value.trim();
    }
}