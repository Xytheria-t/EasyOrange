package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ShippingContact(String address, String phone) {
    public ShippingContact {
        BizRequire.notBlank(address, "收货地址不能为空");
        BizRequire.notBlank(phone, "联系电话不能为空");
    }
}