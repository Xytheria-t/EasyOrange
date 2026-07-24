package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;

public record PaymentMethodInfo(PaymentMethod method) {
    public PaymentMethodInfo {
        if (method == null) {
            throw PaymentDomainException.of("支付方式不能为空");
        }
    }

    public static PaymentMethodInfo of(PaymentMethod method) {
        return new PaymentMethodInfo(method);
    }

    public static PaymentMethodInfo wechat() {
        return new PaymentMethodInfo(PaymentMethod.WECHAT);
    }

    public static PaymentMethodInfo alipay() {
        return new PaymentMethodInfo(PaymentMethod.ALIPAY);
    }

    public static PaymentMethodInfo balance() {
        return new PaymentMethodInfo(PaymentMethod.BALANCE);
    }

    public boolean isWechat() {
        return PaymentMethod.WECHAT == method;
    }

    public boolean isAlipay() {
        return PaymentMethod.ALIPAY == method;
    }

    public boolean isBalance() {
        return PaymentMethod.BALANCE == method;
    }

    public String getDesc() {
        return method.getDesc();
    }

    @Override
    public String toString() {
        return "PaymentMethodInfo[method=" + method + ", desc=" + getDesc() + "]";
    }
}