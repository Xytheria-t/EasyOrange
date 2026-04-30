package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductId(Long value) {
    public ProductId {
        BizRequire.notNull(value, "商品ID不能为空");
        BizRequire.requireTrue(value > 0, "商品ID必须大于0");
    }

    public static ProductId of(Long value) {
        return new ProductId(value);
    }
}
