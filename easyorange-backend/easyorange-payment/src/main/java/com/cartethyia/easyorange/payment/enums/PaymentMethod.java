package com.cartethyia.easyorange.payment.enums;

import com.cartethyia.easyorange.common.util.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum PaymentMethod {

    WECHAT(1, "微信支付"),
    ALIPAY(2, "支付宝"),
    BALANCE(3, "余额支付");

    private final Integer code;
    private final String desc;

    public static PaymentMethod fromCode(Integer code) {
        return EnumUtils.fromCodeSafe(code, values(), PaymentMethod::getCode).orElse(null);
    }

    public static String getDescByCode(Integer code) {
        PaymentMethod method = fromCode(code);
        return method != null ? method.getDesc() : "未知支付方式";
    }
}
