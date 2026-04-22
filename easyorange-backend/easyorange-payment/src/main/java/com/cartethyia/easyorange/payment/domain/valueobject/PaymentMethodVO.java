package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.enums.PaymentMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentMethodVO implements com.cartethyia.easyorange.common.ddd.ValueObject {

    private final Integer code;

    public static PaymentMethodVO of(Integer code) {
        if (code == null) {
            throw BusinessException.of("支付方式不能为空");
        }
        PaymentMethod method = PaymentMethod.fromCode(code);
        if (method == null) {
            throw BusinessException.of("不支持的支付方式: " + code);
        }
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PaymentMethodVO that = (PaymentMethodVO) obj;
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return getDesc() + "(" + code + ")";
    }
}
