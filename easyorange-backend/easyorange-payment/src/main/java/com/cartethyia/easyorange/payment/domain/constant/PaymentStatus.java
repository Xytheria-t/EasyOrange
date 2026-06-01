package com.cartethyia.easyorange.payment.domain.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {

    PENDING(0, "待支付"),
    SUCCESS(1, "已支付"),
    REFUNDED(2, "已退款"),
    PARTIALLY_REFUNDED(3, "部分退款"),
    FAILED(4, "支付失败"),
    CLOSED(5, "已关闭"),
    PAYING(6, "支付中"),
    REFUNDING(7, "退款中");

    @JsonValue
    private final Integer code;
    private final String desc;

    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("PaymentStatus code must not be null");
        }
        for (var status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentStatus code: " + code);
    }

    public static String getDescByCode(Integer code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
