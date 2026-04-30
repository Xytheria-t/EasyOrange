package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record Address(String value) {
    public Address {
        BizRequire.notNull(value, "收货地址不能为空");
        BizRequire.requireTrue(value.length() >= 2, "收货地址长度不能少于2个字符");
    }

    public static Address of(String value) {
        return new Address(value);
    }
}
