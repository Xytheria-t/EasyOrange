package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record PaymentId(String value) {
    public PaymentId {
        BizRequire.notBlank(value, "支付ID不能为空");
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
}
