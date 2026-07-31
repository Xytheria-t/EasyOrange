package com.cartethyia.easyorange.order.domain.constant;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态枚举 — code 为有意义字符串，DB 列 VARCHAR(20)，由 {@code OrderStatusTypeHandler} 持久化。
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

    // === 状态机：单一事实来源 ===
    // 键为当前状态，值为允许到达的目标状态；各转换的触发动作见行内注释。
    // PAID→CANCELLED 仅由管理端强制取消 forceCancel 触发（普通用户取消仅限待付款，见 Order.canCancel()）。
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        PENDING_PAYMENT, Set.of(PAID, CANCELLED),                 // pay / cancel（用户）
        PAID,           Set.of(SHIPPED, CANCELLED, REFUNDED),     // ship / forceCancel（管理端）/ refund
        SHIPPED,        Set.of(COMPLETED, REFUNDED),              // confirmReceipt / refund
        COMPLETED,      Set.of(),                                 // 终端
        CANCELLED,      Set.of(),                                 // 终端
        REFUNDED,       Set.of()                                  // 终端
    );

    public static OrderStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(OrderStatus.class, code);
    }

    /**
     * 从当前状态到目标状态的转换是否合法。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
