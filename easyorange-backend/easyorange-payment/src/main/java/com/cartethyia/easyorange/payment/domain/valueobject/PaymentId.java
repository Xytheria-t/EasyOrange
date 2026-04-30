package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record PaymentId(Long value) {
    public PaymentId {
        BizRequire.notNull(value, "支付ID不能为空");
    }

    public static PaymentId of(Long value) {
        return new PaymentId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
