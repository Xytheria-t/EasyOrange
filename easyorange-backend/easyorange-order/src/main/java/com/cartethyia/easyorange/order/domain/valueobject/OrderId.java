package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record OrderId(Long value) {
    public OrderId {
        BizRequire.notNull(value, "订单ID不能为空");
        BizRequire.positive(value, "订单ID必须为正数");
    }
}