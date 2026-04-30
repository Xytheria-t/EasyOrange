package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record Phone(String value) {
    public Phone {
        BizRequire.notNull(value, "联系电话不能为空");
        BizRequire.requireTrue(value.matches("^1[3-9]\\d{9}$"), "手机号格式不正确");
    }

    public static Phone of(String value) {
        return new Phone(value);
    }
}
