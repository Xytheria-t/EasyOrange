package com.cartethyia.easyorange.payment.domain.constant;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus implements BaseCodeEnum {
    PENDING("PENDING", "待支付"),
    SUCCESS("SUCCESS", "已支付"),
    REFUNDED("REFUNDED", "已退款"),
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED", "部分退款"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭"),
    PAYING("PAYING", "支付中"),
    REFUNDING("REFUNDING", "退款中");

    @JsonValue
    private final String code;

    private final String desc;

    public static PaymentStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(PaymentStatus.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
