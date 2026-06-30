package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record OrderId(String value) {
    public OrderId {
        BizRequire.notBlank(value, "订单ID不能为空");
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }
}
