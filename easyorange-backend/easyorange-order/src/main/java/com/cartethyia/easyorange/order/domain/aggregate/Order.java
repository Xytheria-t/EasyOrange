package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.Version;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.domain.enums.ClosureKind;
import com.cartethyia.easyorange.order.domain.enums.OrderAction;
import com.cartethyia.easyorange.order.domain.enums.OrderStatus;
import com.cartethyia.easyorange.order.domain.enums.PaymentStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderItemRef;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
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
 * 订单聚合根 —— 不可变对象。合法转换的单一事实来源见 {@link OrderAction}，所有转换统一经 {@link #transitionTo} 守卫。
 * <p>
 * 状态机：
 * <pre>
 * PENDING_PAYMENT ──→ PAID ──→ SHIPPED ──→ COMPLETED
 *       │                │         │
 *       ↓                ↓         ↓
 *   CANCELLED        CANCELLED   REFUNDED
 * </pre>
 * 不变量：资产非空且总金额 > 0；认领方不得认领自己的资产；用户取消仅限待付款（已付款走 {@link #forceCancel}）；
 * 取消/退款须附原因。
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
    private final Version version;

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
            LocalDateTime refundTime,
            Version version) {
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
        this.version = version;
    }

    // ── 工厂方法 ──

    /** 认领方等于资产方、资产为空或金额为零时抛 {@code IllegalArgumentException}（聚合根三条创建不变量）。 */
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
                OrderNo.forOrderId(orderId.value()),
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
                null,
                Version.INITIAL);

        List<OrderCreatedEvent.OrderItemPayload> itemPayloads = spec.items().stream()
                .map(item -> new OrderCreatedEvent.OrderItemPayload(
                        item.productId().value(), item.quantity(),
                        item.unitPrice().value(), item.subtotal().value()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UuidV7.generateId(),
                orderId.value(),
                spec.buyerId().value(),
                spec.sellerId().value(),
                itemPayloads,
                totalAmount.value());

        return new Transition<>(aggregate, event);
    }

    // ── 重建 ──

    /** 重建统一入口，列表查询无行项的场景也走这里。 */
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
                spec.refundTime(),
                spec.version());
    }

    // ── 身份判定 ──

    public boolean isBuyer(String userId) {
        return Objects.equals(buyerId.value(), userId);
    }

    public boolean isSeller(String userId) {
        return Objects.equals(sellerId.value(), userId);
    }

    // ── 状态判定 ──
    // 能力谓词只保留有生产调用方的；新增前先确认调用方，否则由
    // OrderAction.X.canApply(status, paymentStatus) 直接裁决，无需在聚合根上重复暴露。

    public boolean canCancel() {
        return OrderAction.CANCEL.canApply(status, paymentStatus);
    }

    public boolean canPay() {
        return OrderAction.PAY.canApply(status, paymentStatus);
    }

    public boolean canConfirmReceipt() {
        return OrderAction.CONFIRM_RECEIPT.canApply(status, paymentStatus);
    }

    // ── 状态迁移 ──

    public Transition<Order, OrderPaidEvent> pay(LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.PAY, null, now),
                new OrderPaidEvent(UuidV7.generateId(), id.value(), buyerId().value()));
    }

    public Transition<Order, OrderCancelledEvent> cancel(String reason, LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.CANCEL, reason, now),
                new OrderCancelledEvent(
                        UuidV7.generateId(), id.value(), buyerId().value(), extractItems(), reason));
    }

    /** 管理端强制取消，允许取消已付款订单（用户取消只限待付款）。 */
    public Transition<Order, OrderCancelledEvent> forceCancel(String reason, LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.FORCE_CANCEL, reason, now),
                new OrderCancelledEvent(
                        UuidV7.generateId(), id.value(), buyerId().value(), extractItems(), reason));
    }

    public Transition<Order, OrderShippedEvent> ship(LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.SHIP, null, now),
                new OrderShippedEvent(UuidV7.generateId(), id.value(), buyerId().value()));
    }

    public Transition<Order, OrderCompletedEvent> confirmReceipt(LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.CONFIRM_RECEIPT, null, now),
                new OrderCompletedEvent(
                        UuidV7.generateId(),
                        id.value(),
                        buyerId().value(),
                        sellerId().value(),
                        extractProductIds()));
    }

    public Transition<Order, OrderRefundedEvent> refund(String reason, LocalDateTime now) {
        return new Transition<>(
                transitionTo(OrderAction.REFUND, reason, now),
                new OrderRefundedEvent(
                        UuidV7.generateId(), id.value(), buyerId().value(), extractItems(), reason));
    }

    // ── 状态机守卫 ──

    /**
     * 状态机守卫 — 所有转换的唯一入口：校验动作在当前 status + paymentStatus 下是否合法、关闭类动作是否附原因，
     * 再一次性应用目标状态 + 目标支付状态 + 按 {@link ClosureKind} 归因的关闭原因/时间。
     * <p>
     * {@code now} 由应用层传入，保证时间源不落在领域模型上。
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

    // ── 内部辅助方法 ──

    private List<String> extractProductIds() {
        return items.stream().map(i -> i.productId().value()).toList();
    }

    /** 资产明细（ID + 数量）— 库存恢复必须按实际数量回补，事件只带 ID 会迫使消费端按 1 猜。 */
    private List<OrderItemRef> extractItems() {
        return items.stream()
                .map(i -> new OrderItemRef(i.productId().value(), i.quantity()))
                .toList();
    }
}
