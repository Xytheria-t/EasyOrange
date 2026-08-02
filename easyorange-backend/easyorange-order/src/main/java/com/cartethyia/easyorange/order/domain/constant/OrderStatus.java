package com.cartethyia.easyorange.order.domain.constant;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态枚举 — code 为有意义字符串，DB 列 VARCHAR(20)，由 {@code OrderStatusTypeHandler} 持久化。
 * <p>
 * 状态机合法转换的**唯一事实来源**是 {@link OrderAction}，本枚举仅声明状态本身；
 * {@link #canTransitionTo(OrderStatus)} 由动作定义派生，避免两份状态机定义漂移。
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum OrderStatus implements BaseCodeEnum {

    // 按生命周期顺序声明：待付款 → 已付款 → 已发货 → 已完成（终端）
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

    /**
     * 从当前状态到目标状态的转换是否合法。
     * <p>
     * 由 {@link OrderAction} 派生：存在任一动作允许从当前状态到达目标状态即为合法。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTransitionTo(OrderStatus target) {
        return Arrays.stream(OrderAction.values())
                .anyMatch(action -> action.sources().contains(this) && action.target() == target);
    }
}
