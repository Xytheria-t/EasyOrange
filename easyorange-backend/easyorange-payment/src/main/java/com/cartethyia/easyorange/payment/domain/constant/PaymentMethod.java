package com.cartethyia.easyorange.payment.domain.constant;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod implements BaseCodeEnum {
    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝"),
    BALANCE("BALANCE", "余额支付");

    @JsonValue
    private final String code;

    private final String desc;

    public static PaymentMethod fromCode(String code) {
        return BaseCodeEnum.fromCode(PaymentMethod.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知支付方式";
        }
    }
}
