package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record Address(String value) {
    public Address {
        BizRequire.notNull(value, "收货地址不能为空");
    }

    public static Address of(String value) {
        return new Address(value);
    }
}
