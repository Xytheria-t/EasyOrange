package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record OrderId(Long value) {
    public OrderId {
        BizRequire.notNull(value, "订单ID不能为空");
        BizRequire.requireTrue(value > 0, "订单ID必须大于0");
    }

    public static OrderId of(Long value) {
        return new OrderId(value);
    }
}
