package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductId(Long value) {
    public ProductId {
        BizRequire.notNull(value, "商品ID不能为空");
        BizRequire.isTrue(value > 0, "商品ID必须为正数");
    }
}