package com.cartethyia.easyorange.order.domain.constant;

import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 订单状态机动作 — 订单生命周期所有合法转换的**唯一事实来源**。
 * <p>
 * 每个动作声明：前置状态集合（sources）、目标状态（target）、目标支付状态（targetPaymentStatus，
 * null 表示不变）、是否需要原因、非法时的错误码，以及额外的支付前置条件（paymentGuard）。
 * {@link OrderStatus#canTransitionTo(OrderStatus)} 由此派生，聚合根统一经
 * {@code Order#transitionTo(OrderAction, String)} 守卫。
 * <pre>
 * PENDING_PAYMENT ──PAY──→ PAID ──SHIP──→ SHIPPED ──CONFIRM_RECEIPT──→ COMPLETED
 *       │                   │  │                     │
 *       │                   │  └──FORCE_CANCEL──→    │
 *       │                   └──REFUND──→            └──REFUND──→
 *   CANCEL──→ CANCELLED            │                              REFUNDED
 *       │        ▲                 │
 *       └────────┴──FORCE_CANCEL───┘
 * </pre>
 * <ul>
 *   <li>{@code CANCEL}：买家取消，仅限待付款</li>
 *   <li>{@code FORCE_CANCEL}：管理端强制取消，待付款或已付款</li>
 *   <li>{@code REFUND}：退款，已付款或已发货，且支付状态必须为已支付</li>
 * </ul>
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum OrderAction {
    PAY(
            "支付",
            Set.of(OrderStatus.PENDING_PAYMENT),
            OrderStatus.PAID,
            PaymentStatus.PAID,
            false,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    CANCEL(
            "取消",
            Set.of(OrderStatus.PENDING_PAYMENT),
            OrderStatus.CANCELLED,
            null,
            true,
            OrderResultCode.ORDER_CANNOT_CANCEL,
            null),
    FORCE_CANCEL(
            "强制取消",
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
            OrderStatus.CANCELLED,
            null,
            true,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    SHIP("发货", Set.of(OrderStatus.PAID), OrderStatus.SHIPPED, null, false, OrderResultCode.ORDER_STATUS_ERROR, null),
    CONFIRM_RECEIPT(
            "确认收货",
            Set.of(OrderStatus.SHIPPED),
            OrderStatus.COMPLETED,
            null,
            false,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    REFUND(
            "退款",
            Set.of(OrderStatus.PAID, OrderStatus.SHIPPED),
            OrderStatus.REFUNDED,
            PaymentStatus.REFUNDED,
            true,
            OrderResultCode.ORDER_CANNOT_REFUND,
            payment -> payment == PaymentStatus.PAID);

    /** 动作名称（用于日志/提示） */
    private final String actionName;
    /** 允许触发该动作的前置状态集合 */
    private final Set<OrderStatus> sources;
    /** 动作执行后的目标状态 */
    private final OrderStatus target;
    /** 动作执行后的目标支付状态；null 表示支付状态不变 */
    private final PaymentStatus targetPaymentStatus;
    /** 是否需要关闭原因（取消/退款等终止性动作），需要时聚合根会记录 cancelReason/cancelTime */
    private final boolean requiresReason;
    /** 非法触发时的错误码 */
    private final OrderResultCode resultCode;
    /** 额外的支付前置条件；null 表示无额外限制 */
    private final Predicate<PaymentStatus> paymentGuard;

    /**
     * 当前订单状态（status + paymentStatus）是否允许触发该动作。
     */
    public boolean canApply(OrderStatus currentStatus, PaymentStatus currentPaymentStatus) {
        return sources.contains(currentStatus) && (paymentGuard == null || paymentGuard.test(currentPaymentStatus));
    }
}
