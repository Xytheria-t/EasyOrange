package com.cartethyia.easyorange.payment.domain.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {

    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝"),
    BALANCE("BALANCE", "余额支付");

    @JsonValue
    private final String code;
    private final String desc;

    public static PaymentMethod fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("PaymentMethod code must not be null");
        }
        for (var method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentMethod code: " + code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知支付方式";
        }
    }
}
