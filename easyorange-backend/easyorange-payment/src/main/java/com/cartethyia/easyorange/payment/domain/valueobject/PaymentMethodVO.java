package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.enums.PaymentMethod;

public record PaymentMethodVO(Integer code) {
    public PaymentMethodVO {
        if (code == null) {
            throw BusinessException.of("支付方式不能为空");
        }
        PaymentMethod method = PaymentMethod.fromCode(code);
        if (method == null) {
            throw BusinessException.of("不支持的支付方式: " + code);
        }
    }

    public static PaymentMethodVO of(Integer code) {
        return new PaymentMethodVO(code);
    }

    public static PaymentMethodVO wechat() {
        return new PaymentMethodVO(PaymentMethod.WECHAT.getCode());
    }

    public static PaymentMethodVO alipay() {
        return new PaymentMethodVO(PaymentMethod.ALIPAY.getCode());
    }

    public static PaymentMethodVO balance() {
        return new PaymentMethodVO(PaymentMethod.BALANCE.getCode());
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
        return "PaymentMethodVO[code=" + code + ", desc=" + getDesc() + "]";
    }
}