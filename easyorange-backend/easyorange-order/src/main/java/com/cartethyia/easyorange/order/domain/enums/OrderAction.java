package com.cartethyia.easyorange.order.domain.enums;

import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 订单状态机动作 — 生命周期合法转换的唯一事实来源：每个动作声明前置状态集、目标状态、目标支付状态（null
 * 表示不变）、关闭归因（NONE / CANCEL / REFUND）与非法时的错误码；是否可触发只由 {@link #canApply} 裁决，
 * 聚合根统一经 {@code Order#transitionTo} 守卫，新增转换必须先在此声明，禁止绕过守卫改状态。
 * <p>
 * 主干：PENDING_PAYMENT ─PAY→ PAID ─SHIP→ SHIPPED ─CONFIRM_RECEIPT→ COMPLETED
 * <p>
 * 旁路判据：CANCEL 与 FORCE_CANCEL 归 CANCELLED（前者限待付款、后者覆盖待付款与已付款）；REFUND 归 REFUNDED，
 * 要求支付状态为已支付。
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
            ClosureKind.NONE,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    CANCEL(
            "取消",
            Set.of(OrderStatus.PENDING_PAYMENT),
            OrderStatus.CANCELLED,
            null,
            ClosureKind.CANCEL,
            OrderResultCode.ORDER_CANNOT_CANCEL,
            null),
    FORCE_CANCEL(
            "强制取消",
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
            OrderStatus.CANCELLED,
            null,
            ClosureKind.CANCEL,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    SHIP(
            "发货",
            Set.of(OrderStatus.PAID),
            OrderStatus.SHIPPED,
            null,
            ClosureKind.NONE,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    CONFIRM_RECEIPT(
            "确认收货",
            Set.of(OrderStatus.SHIPPED),
            OrderStatus.COMPLETED,
            null,
            ClosureKind.NONE,
            OrderResultCode.ORDER_STATUS_ERROR,
            null),
    REFUND(
            "退款",
            Set.of(OrderStatus.PAID, OrderStatus.SHIPPED),
            OrderStatus.REFUNDED,
            PaymentStatus.REFUNDED,
            ClosureKind.REFUND,
            OrderResultCode.ORDER_CANNOT_REFUND,
            payment -> payment == PaymentStatus.PAID);

    private final String actionName;
    private final Set<OrderStatus> sources;
    private final OrderStatus target;
    private final PaymentStatus targetPaymentStatus;
    private final ClosureKind closureKind;
    private final OrderResultCode resultCode;
    private final Predicate<PaymentStatus> paymentGuard;

    public boolean canApply(OrderStatus currentStatus, PaymentStatus currentPaymentStatus) {
        return sources.contains(currentStatus) && (paymentGuard == null || paymentGuard.test(currentPaymentStatus));
    }
}
