package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductTitle(String value) {

    public static final int MAX_LENGTH = 200;

    public ProductTitle {
        BizRequire.notBlank(value, "商品名称不能为空");
        value = value.trim();
        BizRequire.requireTrue(
                value.length() <= MAX_LENGTH,
                "商品名称长度必须在 1-" + MAX_LENGTH + " 个字符之间"
        );
    }

    public static ProductTitle of(String value) {
        return new ProductTitle(value);
    }
}
