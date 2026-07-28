package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderStatusException;
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
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>订单必须包含至少一件资产，总金额必须大于 0</li>
 *   <li>认领方不能认领自己的资产</li>
 *   <li>状态转换必须严格遵循状态机规则</li>
 *   <li>取消/退款时必须附带原因</li>
 * </ul>
 */
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

    private Order(OrderId id, OrderNo orderNo, UserId buyerId, UserId sellerId,
                           List<OrderItem> items, Money totalAmount, OrderStatus status,
                           PaymentStatus paymentStatus, Address address, Phone phone,
                           String remark, String cancelReason, LocalDateTime cancelTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.items = items != null ? List.copyOf(items) : List.of();
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.address = address;
        this.phone = phone;
        this.remark = remark;
        this.cancelReason = cancelReason;
        this.cancelTime = cancelTime;
    }

    // ==================== Getters ====================

    public OrderId id() { return id; }
    public OrderNo orderNo() { return orderNo; }
    public UserId buyerId() { return buyerId; }
    public UserId sellerId() { return sellerId; }
    public List<OrderItem> items() { return List.copyOf(items); }
    public Money totalAmount() { return totalAmount; }
    public OrderStatus status() { return status; }
    public PaymentStatus paymentStatus() { return paymentStatus; }
    public Address address() { return address; }
    public Phone phone() { return phone; }
    public String remark() { return remark; }
    public String cancelReason() { return cancelReason; }
    public LocalDateTime cancelTime() { return cancelTime; }

    // ==================== Factory ====================

    /**
     * 创建新订单。
     *
     * @param spec 创建参数（收敛 buyerId/sellerId/items/address/phone/remark/orderId）
     * @return 订单创建结果（含聚合根与领域事件）
     * @throws IllegalArgumentException 如果认领方等于资产方、资产为空或金额为零
     */
    public static OrderTransition<OrderCreatedEvent> createOrder(OrderCreateSpec spec) {
        BizRequire.requireTrue(!java.util.Objects.equals(spec.buyerId().value(), spec.sellerId().value()),
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
                spec.address(), spec.phone(), spec.remark(), null, null
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

        return new OrderTransition<>(aggregate, event);
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
                spec.address(), spec.phone(), spec.remark(), spec.cancelReason(), spec.cancelTime()
        );
    }

    // ==================== Status Queries ====================

    /** 是否可支付（仅待付款状态可支付） */
    public boolean canPay() { return status == OrderStatus.PENDING_PAYMENT; }

    /** 是否可取消（仅待付款状态可取消） */
    public boolean canCancel() { return status == OrderStatus.PENDING_PAYMENT; }

    /** 是否可发货（仅已付款状态可发货） */
    public boolean canShip() { return status == OrderStatus.PAID; }

    /** 是否可确认收货（仅已发货状态可确认） */
    public boolean canConfirmReceipt() { return status == OrderStatus.SHIPPED; }

    /** 是否可退款（已付款或已发货状态，且支付状态为已支付时可退款） */
    public boolean canRefund() {
        return (status == OrderStatus.PAID || status == OrderStatus.SHIPPED)
                && paymentStatus == PaymentStatus.PAID;
    }

    // ==================== State Transitions ====================

    /** 支付订单 */
    public OrderTransition<OrderPaidEvent> pay() {
        BizRequire.requireTrue(canPay(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = copy(OrderStatus.PAID, PaymentStatus.PAID, cancelReason, cancelTime);
        return new OrderTransition<>(updated, new OrderPaidEvent(id.value(), PaymentStatus.PAID.getCode()));
    }

    /** 取消订单 */
    public OrderTransition<OrderCancelledEvent> cancel(String reason) {
        BizRequire.requireTrue(canCancel(), OrderResultCode.ORDER_CANNOT_CANCEL);
        var updated = copy(OrderStatus.CANCELLED, paymentStatus, reason, LocalDateTime.now());
        return new OrderTransition<>(updated, new OrderCancelledEvent(id.value(), extractProductIds(), reason));
    }

    /**
     * 管理端强制取消订单 — 允许取消已付款的订单。
     * <p>
     * 正常用户取消只允许待付款订单，管理端可以强制取消已付款订单。
     */
    public OrderTransition<OrderCancelledEvent> forceCancel(String reason) {
        if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PAID) {
            throw new OrderStatusException(id.value(), "强制取消", status);
        }
        var updated = copy(OrderStatus.CANCELLED, paymentStatus, reason, LocalDateTime.now());
        return new OrderTransition<>(updated, new OrderCancelledEvent(id.value(), extractProductIds(), reason));
    }

    /** 发货 */
    public OrderTransition<OrderShippedEvent> ship() {
        BizRequire.requireTrue(canShip(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = copy(OrderStatus.SHIPPED, paymentStatus, cancelReason, cancelTime);
        return new OrderTransition<>(updated, new OrderShippedEvent(id.value()));
    }

    /** 确认收货 */
    public OrderTransition<OrderCompletedEvent> confirmReceipt() {
        BizRequire.requireTrue(canConfirmReceipt(), OrderResultCode.ORDER_STATUS_ERROR);
        var updated = copy(OrderStatus.COMPLETED, paymentStatus, cancelReason, cancelTime);
        return new OrderTransition<>(updated, new OrderCompletedEvent(id.value(), extractProductIds()));
    }

    /** 退款 */
    public OrderTransition<OrderRefundedEvent> refund(String reason) {
        BizRequire.requireTrue(canRefund(), OrderResultCode.ORDER_CANNOT_REFUND);
        var updated = copy(OrderStatus.REFUNDED, PaymentStatus.REFUNDED, reason, LocalDateTime.now());
        return new OrderTransition<>(updated, new OrderRefundedEvent(id.value(), extractProductIds(), reason));
    }

    // ==================== Internal Helpers ====================

    /**
     * 仅变更订单状态、支付状态、取消原因和取消时间，其余字段保持不变。
     * 用于安全的不可变状态复制（替代多个 withXxx 方法）。
     */
    private Order copy(OrderStatus newStatus, PaymentStatus newPaymentStatus,
                                 String newCancelReason, LocalDateTime newCancelTime) {
        return new Order(id, orderNo, buyerId, sellerId, items,
                totalAmount, newStatus, newPaymentStatus,
                address, phone, remark, newCancelReason, newCancelTime);
    }

    private List<String> extractProductIds() {
        return items.stream().map(i -> i.productId().value()).toList();
    }

    // ==================== Result Record ====================

    /**
     * 状态转换结果 — 聚合根新实例 + 领域事件。
     * <p>
     * 泛型 {@code <E>} 保留具体事件类型，避免调用方 cast。
     *
     * @param <E> 领域事件具体类型
     */
    public record OrderTransition<E extends DomainEvent>(Order aggregate, E event) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", orderNo=" + orderNo + ", status=" + status + ", paymentStatus=" + paymentStatus + "}";
    }
}
