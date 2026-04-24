package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record SellerId(Long value) {
    public SellerId {
        BizRequire.notNull(value, "卖家ID不能为空");
        BizRequire.isTrue(value > 0, "卖家ID必须为正数");
    }
}