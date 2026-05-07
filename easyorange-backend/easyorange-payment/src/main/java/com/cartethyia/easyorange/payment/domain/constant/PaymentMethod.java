package com.cartethyia.easyorange.payment.domain.constant;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {

    WECHAT(1, "微信支付"),
    ALIPAY(2, "支付宝"),
    BALANCE(3, "余额支付");

    private final Integer code;
    private final String desc;

    public static PaymentMethod fromCode(Integer code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

    public static String getDescByCode(Integer code) {
        PaymentMethod method = fromCode(code);
        return method != null ? method.getDesc() : "未知支付方式";
    }
}
