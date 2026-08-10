package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.domain.constant.ClosureKind;
import com.cartethyia.easyorange.order.domain.constant.OrderAction;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 订单聚合根 —— 不可变对象
 * <p>
 * 订单遵循以下状态机：
 * <pre>
 * PENDING_PAYMENT ──→ PAID ──→ SHIPPED ──→ COMPLETED
 *       │                │         │
 *       ↓                ↓         ↓
 *   CANCELLED        CANCELLED   REFUNDED
 * </pre>
 * 状态机合法转换的单一事实来源见 {@link OrderAction}，所有转换统一经
 * {@link #transitionTo(OrderAction, String, LocalDateTime)} 守卫（一处校验合法性 + 一处应用副作用）。
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>订单必须包含至少一件资产，总金额必须大于 0</li>
 *   <li>认领方不能认领自己的资产</li>
 *   <li>状态转换必须严格遵循状态机规则（用户取消仅限待付款，已付款取消走 forceCancel）</li>
 *   <li>取消/退款时必须附带原因</li>
 * </ul>
 */
@Getter
@Accessors(fluent = true)
@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "orderNo", "status", "paymentStatus"})
public class Order {

    private final OrderId id;
    private final OrderNo orderNo;
    private final UserId buyerId;
    private final UserId sellerId;
    private final List<OrderItem> items;
    private final Money totalAmount;
    private final OrderStatus status;
    private final PaymentStatus paymentStatus;
    private final Address address;
    private final Phone phone;
    private final String remark;
    private final String cancelReason;
    private final LocalDateTime cancelTime;
    private final String refundReason;
    private final LocalDateTime refundTime;

    private Order(
            OrderId id,
            OrderNo orderNo,
            UserId buyerId,
            UserId sellerId,
            List<OrderItem> items,
            Money totalAmount,
            OrderStatus status,
            PaymentStatus paymentStatus,
            Address address,
            Phone phone,
            String remark,
            String cancelReason,
            LocalDateTime cancelTime,
            String refundReason,
            LocalDateTime refundTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.items = items != null ? List.copyOf(items) : List.of();
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.address = address;
        this.phone = phone;
        this.remark = remark;
        this.cancelReason = cancelReason;
        this.cancelTime = cancelTime;
        this.refundReason = refundReason;
        this.refundTime = refundTime;
    }

    // ==================== Factory ====================

    /**
     * 创建新订单。
     *
     * @param spec 创建参数（收敛 buyerId/sellerId/items/address/phone/remark/orderId）
     * @return 订单创建结果（含聚合根与领域事件）
     * @throws IllegalArgumentException 如果认领方等于资产方、资产为空或金额为零
     */
    public static Transition<Order, OrderCreatedEvent> createOrder(OrderCreateSpec spec) {
        BizRequire.requireTrue(
                !Objects.equals(spec.buyerId().value(), spec.sellerId().value()), "不能认领自己的资产");
        BizRequire.notEmpty(spec.items(), "订单资产不能为空");

        BigDecimal total =
                spec.items().stream().map(item -> item.subtotal().value()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BizRequire.requireTrue(total.compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");
        Money totalAmount = Money.of(total);

        OrderId orderId = spec.orderId();
        Order aggregate = new Order(
                orderId,
                OrderNo.of("ORD" + orderId.value()),
                spec.buyerId(),
                spec.sellerId(),
                spec.items(),
                totalAmount,
                OrderStatus.PENDING_PAYMENT,
                PaymentStatus.UNPAID,
                spec.address(),
                spec.phone(),
                spec.remark(),
                null,
                null,
                null,
                null);

        List<OrderCreatedEvent.OrderItemPayload> itemPayloads = spec.items().stream()
                .map(item -> new OrderCreatedEvent.OrderItemPayload(
                        item.productId().value(), item.quantity(),
                        item.unitPrice().value(), item.subtotal().value()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId.value(), spec.buyerId().value(), spec.sellerId().value(), itemPayloads, totalAmount.value());

        return new Transition<>(aggregate, event);
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层重建聚合根（统一入口，含列表查询无行项场景）。
     * <p>
     * 状态字段使用领域枚举类型，由 TypeHandler 完成 VARCHAR 列互转。
     */
    public static Order from(OrderReconstructSpec spec) {
        return new Order(
                spec.id(),
                spec.orderNo(),
                spec.buyerId(),
                spec.sellerId(),
                spec.items(),
                spec.totalAmount(),
                spec.status(),
                spec.paymentStatus(),
                spec.address(),
                spec.phone(),
                spec.remark(),
                spec.cancelReason(),
                spec.cancelTime(),
                spec.refundReason(),
                spec.refundTime());
    }

    // ==================== Status Queries ====================
    // 仅保留有生产调用方的谓词；其余能力查询（canPay/canShip/...）在需要时由
    // OrderAction.X.canApply(status, paymentStatus) 直接裁决，无需在聚合根上重复暴露。

    /** 是否可取消（买家取消仅限待付款状态；已付款订单取消走 {@link #forceCancel}） */
    public boolean canCancel() {
        return OrderAction.CANCEL.canApply(status, paymentStatus);
    }

    /** 是否可确认收货（仅已发货状态可确认） */
    public boolean canConfirmReceipt() {
        return OrderAction.CONFIRM_RECEIPT.canApply(status, paymentStatus);
    }

    // ==================== State Transitions ====================

    /** 支付订单 */
    public Transition<Order, OrderPaidEvent> pay(LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.PAY, null, now), new OrderPaidEvent(id.value(), PaymentStatus.PAID.getCode()));
    }

    /** 取消订单（买家路径，仅限待付款） */
    public Transition<Order, OrderCancelledEvent> cancel(String reason, LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.CANCEL, reason, now),
                new OrderCancelledEvent(id.value(), extractProductIds(), reason));
    }

    /**
     * 管理端强制取消订单 — 允许取消已付款的订单。
     * <p>
     * 正常用户取消只允许待付款订单，管理端可以强制取消已付款订单。
     */
    public Transition<Order, OrderCancelledEvent> forceCancel(String reason, LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.FORCE_CANCEL, reason, now),
                new OrderCancelledEvent(id.value(), extractProductIds(), reason));
    }

    /** 发货 */
    public Transition<Order, OrderShippedEvent> ship(LocalDateTime now) {
        return new Transition<>(transitionTo(OrderAction.SHIP, null, now), new OrderShippedEvent(id.value()));
    }

    /** 确认收货 */
    public Transition<Order, OrderCompletedEvent> confirmReceipt(LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.CONFIRM_RECEIPT, null, now),
                new OrderCompletedEvent(id.value(), extractProductIds()));
    }

    /** 退款 */
    public Transition<Order, OrderRefundedEvent> refund(String reason, LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.REFUND, reason, now),
                new OrderRefundedEvent(id.value(), extractProductIds(), reason));
    }

    // ==================== State Machine Guard ====================

    /**
     * 状态机守卫 — 所有转换的唯一入口。
     * <p>
     * 校验动作在当前订单状态（status + paymentStatus）下是否合法、关闭类动作是否附带原因，
     * 然后一次性应用副作用：目标状态 + 目标支付状态 + 按 {@link ClosureKind} 归因的关闭原因/时间。
     * 任何新增转换都必须先声明 {@link OrderAction}，再经此方法执行，禁止绕过守卫直接修改状态。
     *
     * @param now 关闭类动作的归因时间（由应用层传入，保证时间源不落在领域模型上）
     */
    private Order transitionTo(OrderAction action, String reason, LocalDateTime now) {
        BizRequire.requireTrue(action.canApply(status, paymentStatus), action.resultCode());
        BizRequire.requireTrue(
                action.closureKind() == ClosureKind.NONE || (reason != null && !reason.isBlank()), action.resultCode());
        var builder = toBuilder()
                .status(action.target())
                .paymentStatus(action.targetPaymentStatus() != null ? action.targetPaymentStatus() : paymentStatus);
        switch (action.closureKind()) {
            case CANCEL -> builder = builder.cancelReason(reason).cancelTime(now);
            case REFUND -> builder = builder.refundReason(reason).refundTime(now);
            case NONE -> {}
        }
        return builder.build();
    }

    // ==================== Internal Helpers ====================

    private List<String> extractProductIds() {
        return items.stream().map(i -> i.productId().value()).toList();
    }
}
