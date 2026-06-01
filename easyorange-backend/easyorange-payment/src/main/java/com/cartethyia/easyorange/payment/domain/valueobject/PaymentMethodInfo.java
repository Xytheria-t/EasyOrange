package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;

public record PaymentMethodInfo(Integer code) {
    public PaymentMethodInfo {
        if (code == null) {
            throw PaymentDomainException.of("支付方式不能为空");
        }
        try {
            PaymentMethod.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw PaymentDomainException.of("不支持的支付方式: " + code);
        }
    }

    public static PaymentMethodInfo of(Integer code) {
        return new PaymentMethodInfo(code);
    }

    public static PaymentMethodInfo wechat() {
        return new PaymentMethodInfo(PaymentMethod.WECHAT.getCode());
    }

    public static PaymentMethodInfo alipay() {
        return new PaymentMethodInfo(PaymentMethod.ALIPAY.getCode());
    }

    public static PaymentMethodInfo balance() {
        return new PaymentMethodInfo(PaymentMethod.BALANCE.getCode());
    }

    public boolean isWechat() {
        return PaymentMethod.WECHAT.getCode().equals(code);
    }

    public boolean isAlipay() {
        return PaymentMethod.ALIPAY.getCode().equals(code);
    }

    public boolean isBalance() {
        return PaymentMethod.BALANCE.getCode().equals(code);
    }

    public String getDesc() {
        return PaymentMethod.getDescByCode(code);
    }

    @Override
    public String toString() {
        return "PaymentMethodInfo[code=" + code + ", desc=" + getDesc() + "]";
    }
}