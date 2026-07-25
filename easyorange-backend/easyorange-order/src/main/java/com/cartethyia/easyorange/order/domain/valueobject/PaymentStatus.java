package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举 — 订单聚合根中使用的值对象。
 * <p>
 * code 为有意义字符串，DB 列 VARCHAR(20)，由 {@code PaymentStatusTypeHandler} 持久化。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus implements BaseCodeEnum {

    /** 未支付 */
    UNPAID("UNPAID", "未支付"),

    /** 已支付 */
    PAID("PAID", "已支付"),

    /** 已退款 */
    REFUNDED("REFUNDED", "已退款");

    @JsonValue
    private final String code;
    private final String desc;

    public static PaymentStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(PaymentStatus.class, code);
    }
}
