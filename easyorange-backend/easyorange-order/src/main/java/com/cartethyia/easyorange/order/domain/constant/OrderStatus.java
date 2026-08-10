package com.cartethyia.easyorange.order.domain.constant;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
     * 仅按状态维度判断转换是否可达 —— 由 {@link OrderAction} 派生：存在任一动作以本状态为前置、
     * 以目标为去向即为合法。
     * <p>
     * 注意这是**状态子集投影**：忽略支付维度（paymentGuard）。需要含支付约束的完整判定请用
     * {@link OrderAction#canApply(OrderStatus, com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus)}，
     * 二者可能给出不同结论
     * （如 UNPAID 订单在状态维度可达 REFUNDED，但 canApply 会拒绝）。
     */
    public boolean canTransitionTo(OrderStatus target) {
        return Arrays.stream(OrderAction.values())
                .anyMatch(action -> action.sources().contains(this) && action.target() == target);
    }
}
