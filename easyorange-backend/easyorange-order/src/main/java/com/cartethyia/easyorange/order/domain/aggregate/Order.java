package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
 * 状态机合法转换的单一事实来源见 {@link OrderStatus#canTransitionTo(OrderStatus)}。
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
@Builder(toBuilder = true)
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
    private final Clock clock;

    private Order(OrderId id, OrderNo orderNo, UserId buyerId, UserId sellerId,
                  List<OrderItem> items, Money totalAmount, OrderStatus status,
                  PaymentStatus paymentStatus, Address address, Phone phone,
                  String remark, String cancelReason, LocalDateTime cancelTime, Clock clock) {
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
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
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
        BizRequire.requireTrue(!Objects.equals(spec.buyerId().value(), spec.sellerId().value()),
                "不能认领自己的资产");
        BizRequire.notEmpty(spec.items(), "订单资产不能为空");

        BigDecimal total = spec.items().stream()
                .map(item -> item.subtotal().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BizRequire.requireTrue(total.compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");
        Money totalAmount = Money.of(total);

        OrderId orderId = OrderId.of(spec.orderId());
        Order aggregate = new Order(
                orderId, OrderNo.of(spec.orderId()), spec.buyerId(), spec.sellerId(), spec.items(),
                totalAmount, OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID,
                spec.address(), spec.phone(), spec.remark(), null, null, null
        );

        List<OrderCreatedEvent.OrderItemPayload> itemPayloads = spec.items().stream()
                .map(item -> new OrderCreatedEvent.OrderItemPayload(
                        item.productId().value(), item.quantity(),
                        item.unitPrice().value(), item.subtotal().value()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                spec.orderId(), spec.buyerId().value(), spec.sellerId().value(),
                itemPayloads, totalAmount.value()
        );

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
                spec.id(), spec.orderNo(), spec.buyerId(), spec.sellerId(),
                spec.items(), spec.totalAmount(), spec.status(), spec.paymentStatus(),
                spec.address(), spec.phone(), spec.remark(), spec.cancelReason(), spec.cancelTime(), null
        );
    }

    // ==================== Status Queries ====================

    /** 是否可支付（仅待付款状态可支付） */
    public boolean canPay() { return status.canTransitionTo(OrderStatus.PAID); }

    /**
     * 是否可取消（用户取消仅限待付款状态；已付款订单取消走 {@link #forceCancel}）。
     */
    public boolean canCancel() { return status == OrderStatus.PENDING_PAYMENT; }

    /** 是否可强制取消（待付款或已付款状态，管理端路径） */
    public boolean canForceCancel() { return status.canTransitionTo(OrderStatus.CANCELLED); }

    /** 是否可发货（仅已付款状态可发货） */
    public boolean canShip() { return status.canTransitionTo(OrderStatus.SHIPPED); }

    /** 是否可确认收货（仅已发货状态可确认） */
    public boolean canConfirmReceipt() { return status.canTransitionTo(OrderStatus.COMPLETED); }

    /** 是否可退款（已付款或已发货状态，且支付状态为已支付时可退款） */
    public boolean canRefund() {
        return status.canTransitionTo(OrderStatus.REFUNDED) && paymentStatus == PaymentStatus.PAID;
    }

    // ==================== State Transitions ====================

    /** 支付订单 */
    public Transition<Order, OrderPaidEvent> pay() {
        BizRequire.requireTrue(canPay(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = toBuilder().status(OrderStatus.PAID).paymentStatus(PaymentStatus.PAID).build();
        return new Transition<>(updated, new OrderPaidEvent(id.value(), PaymentStatus.PAID.getCode()));
    }

    /** 取消订单 */
    public Transition<Order, OrderCancelledEvent> cancel(String reason) {
        BizRequire.requireTrue(canCancel(), OrderResultCode.ORDER_CANNOT_CANCEL);
        var updated = toBuilder().status(OrderStatus.CANCELLED)
                .cancelReason(reason).cancelTime(now()).build();
        return new Transition<>(updated, new OrderCancelledEvent(id.value(), extractProductIds(), reason));
    }

    /**
     * 管理端强制取消订单 — 允许取消已付款的订单。
     * <p>
     * 正常用户取消只允许待付款订单，管理端可以强制取消已付款订单。
     */
    public Transition<Order, OrderCancelledEvent> forceCancel(String reason) {
        BizRequire.requireTrue(canForceCancel(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = toBuilder().status(OrderStatus.CANCELLED)
                .cancelReason(reason).cancelTime(now()).build();
        return new Transition<>(updated, new OrderCancelledEvent(id.value(), extractProductIds(), reason));
    }

    /** 发货 */
    public Transition<Order, OrderShippedEvent> ship() {
        BizRequire.requireTrue(canShip(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = toBuilder().status(OrderStatus.SHIPPED).build();
        return new Transition<>(updated, new OrderShippedEvent(id.value()));
    }

    /** 确认收货 */
    public Transition<Order, OrderCompletedEvent> confirmReceipt() {
        BizRequire.requireTrue(canConfirmReceipt(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = toBuilder().status(OrderStatus.COMPLETED).build();
        return new Transition<>(updated, new OrderCompletedEvent(id.value(), extractProductIds()));
    }

    /** 退款 */
    public Transition<Order, OrderRefundedEvent> refund(String reason) {
        BizRequire.requireTrue(canRefund(), OrderResultCode.ORDER_CANNOT_REFUND);
        var updated = toBuilder().status(OrderStatus.REFUNDED).paymentStatus(PaymentStatus.REFUNDED)
                .cancelReason(reason).cancelTime(now()).build();
        return new Transition<>(updated, new OrderRefundedEvent(id.value(), extractProductIds(), reason));
    }

    // ==================== Internal Helpers ====================

    /** 取消/退款时间取自注入的 {@link Clock}（测试可注入固定时钟保证确定性）。 */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private List<String> extractProductIds() {
        return items.stream().map(i -> i.productId().value()).toList();
    }
}
