package com.cartethyia.easyorange.order.domain.valueobject;

import java.util.Arrays;

/**
 * 支付状态枚举 —— 订单聚合根中使用的值对象
 * <p>
 * 替代原始 Integer 类型，提供类型安全和语义明确的支付状态表示。
 * </p>
 */
public enum PaymentStatus {

    /** 未支付 */
    UNPAID(0, "未支付"),

    /** 已支付 */
    PAID(1, "已支付"),

    /** 已退款 */
    REFUNDED(2, "已退款");

    private final int code;
    private final String desc;

    PaymentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int code() {
        return code;
    }

    public String desc() {
        return desc;
    }

    public static PaymentStatus of(int code) {
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment status code: " + code));
    }

    public static PaymentStatus of(Integer code) {
        if (code == null) {
            return UNPAID;
        }
        return of(code.intValue());
    }
}
