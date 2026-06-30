package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ProductId(String value) {
    public ProductId {
        BizRequire.notBlank(value, "资产ID不能为空");
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }
}
