package com.cartethyia.easyorange.order.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 支付状态枚举 —— 订单聚合根中使用的值对象
 * <p>
 * 替代原始 Integer 类型，提供类型安全和语义明确的支付状态表示。
 * </p>
 */
public enum PaymentStatus {

    /** 未支付 */
    UNPAID("0", "未支付"),

    /** 已支付 */
    PAID("1", "已支付"),

    /** 已退款 */
    REFUNDED("2", "已退款");

    @JsonValue
    private final String code;
    private final String desc;

    PaymentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String code() {
        return code;
    }

    public String desc() {
        return desc;
    }

    public static PaymentStatus fromCode(String code) {
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
}
