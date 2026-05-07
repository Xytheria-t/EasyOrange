package com.cartethyia.easyorange.payment.domain.constant;

import java.util.Arrays;
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

    private final Integer code;
    private final String desc;

    public static PaymentStatus fromCode(Integer code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

    public static String getDescByCode(Integer code) {
        PaymentStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }
}
