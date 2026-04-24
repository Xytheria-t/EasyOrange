package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record OrderProductId(Long value) {
    public OrderProductId {
        BizRequire.notNull(value, "商品ID不能为空");
        BizRequire.isTrue(value > 0, "商品ID必须为正数");
    }
}