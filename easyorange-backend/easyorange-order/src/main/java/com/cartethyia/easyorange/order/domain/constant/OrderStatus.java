package com.cartethyia.easyorange.order.domain.constant;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举 — code 为有意义字符串，DB 列 VARCHAR(20)，由 {@code OrderStatusTypeHandler} 持久化。
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum OrderStatus implements BaseCodeEnum {

    PENDING_PAYMENT("PENDING_PAYMENT", "待付款"),
    PAID("PAID", "已付款"),
    SHIPPED("SHIPPED", "已发货"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    REFUNDED("REFUNDED", "已退款");

    @JsonValue
    private final String code;
    private final String desc;

    public static OrderStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(OrderStatus.class, code);
    }
}
