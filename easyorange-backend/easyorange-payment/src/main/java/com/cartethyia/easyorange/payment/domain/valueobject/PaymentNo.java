package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record PaymentNo(String value) {
    public PaymentNo {
        BizRequire.notNull(value, "支付编号不能为空");
        BizRequire.requireTrue(value.startsWith("PAY"), "支付编号格式无效");
    }

    public static PaymentNo of(String value) {
        return new PaymentNo(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
