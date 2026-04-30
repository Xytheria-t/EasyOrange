package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record OrderNo(String value) {
    public OrderNo {
        BizRequire.notNull(value, "订单编号不能为空");
        BizRequire.requireTrue(value.startsWith("ORD"), "订单编号格式不正确");
    }

    public static OrderNo of(Long orderId) {
        return new OrderNo("ORD" + orderId);
    }

    public static OrderNo of(String value) {
        return new OrderNo(value);
    }
}
