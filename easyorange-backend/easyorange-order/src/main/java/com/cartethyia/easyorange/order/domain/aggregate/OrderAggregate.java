package com.cartethyia.easyorange.order.domain.aggregate;

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
import com.cartethyia.easyorange.common.domain.Money;
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
 *   <li>订单必须包含至少一件商品，总金额必须大于 0</li>
 *   <li>买家不能购买自己的商品</li>
 *   <li>状态转换必须严格遵循状态机规则</li>
 *   <li>取消/退款时必须附带原因</li>
 * </ul>
 */
public class OrderAggregate {

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

    private OrderAggregate(OrderId id, OrderNo orderNo, UserId buyerId, UserId sellerId,
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

    public OrderId id() {
        return id;
    }

    public OrderNo orderNo() {
        return orderNo;
    }

    public UserId buyerId() {
        return buyerId;
    }

    public UserId sellerId() {
        return sellerId;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public OrderStatus status() {
        return status;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public Address address() {
        return address;
    }

    public Phone phone() {
        return phone;
    }

    public String remark() {
        return remark;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public LocalDateTime cancelTime() {
        return cancelTime;
    }

    // ==================== Factory ====================

    /**
     * 创建新订单
     *
     * @param buyerId  买家 ID
     * @param sellerId 卖家 ID
     * @param items    订单商品列表
     * @param address  收货地址
     * @param phone    联系电话
     * @param remark   备注
     * @return 订单创建结果（含聚合根与领域事件）
     * @throws IllegalArgumentException 如果买家等于卖家、商品为空或金额为零
     */
    public static OrderCreatedResult createOrder(UserId buyerId, UserId sellerId,
                                                  List<OrderItem> items,
                                                  Address address, Phone phone, String remark,
                                                  Long orderId) {
        BizRequire.ne(buyerId.value(), sellerId.value(), "不能购买自己的商品");
        BizRequire.notEmpty(items, "订单商品不能为空");

        BigDecimal total = items.stream()
                .map(item -> item.subtotal().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BizRequire.requireTrue(total.compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");
        Money totalAmount = Money.of(total);

        OrderNo orderNo = OrderNo.of(orderId);

        OrderAggregate aggregate = new OrderAggregate(
                OrderId.of(orderId), orderNo, buyerId, sellerId, items,
                totalAmount, OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID,
                address, phone, remark, null, null
        );

        List<OrderCreatedEvent.OrderItemPayload> itemPayloads = items.stream()
                .map(item -> new OrderCreatedEvent.OrderItemPayload(
                        item.productId().value(), item.quantity(),
                        item.unitPrice().value(), item.subtotal().value()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, buyerId.value(), sellerId.value(),
                itemPayloads, totalAmount.value()
        );

        return new OrderCreatedResult(aggregate, event);
    }

    // ==================== Reconstruction ====================

    /**
     * 从完整的值对象重建聚合根（主要用于测试和已有聚合根重建）。
     */
    public static OrderAggregate from(OrderId id, OrderNo orderNo, UserId buyerId, UserId sellerId,
                                       List<OrderItem> items, Money totalAmount, OrderStatus status,
                                       PaymentStatus paymentStatus, Address address, Phone phone,
                                       String remark, String cancelReason, LocalDateTime cancelTime) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, items,
                totalAmount, status, paymentStatus, address, phone,
                remark, cancelReason, cancelTime);
    }

    /**
     * 从持久层原始数据重建聚合根（不含行项）。
     *
     * @see #fromRaw(Long, String, Long, Long, List, BigDecimal, Integer, Integer, String, String, String, String, LocalDateTime)
     */
    public static OrderAggregate fromRaw(Long id, String orderNo, Long buyerId, Long sellerId,
                                          BigDecimal amount, Integer status, Integer paymentStatus,
                                          String address, String phone, String remark,
                                          String cancelReason, LocalDateTime cancelTime) {
        return fromRaw(id, orderNo, buyerId, sellerId, List.of(),
                amount, status, paymentStatus, address, phone,
                remark, cancelReason, cancelTime);
    }

    /**
     * 从持久层原始数据重建聚合根（含行项）。
     */
    public static OrderAggregate fromRaw(Long id, String orderNo, Long buyerId, Long sellerId,
                                          List<OrderItem> items, BigDecimal totalAmount,
                                          Integer status, Integer paymentStatus,
                                          String address, String phone, String remark,
                                          String cancelReason, LocalDateTime cancelTime) {
        return new OrderAggregate(
                OrderId.of(id), OrderNo.of(orderNo), UserId.of(buyerId), UserId.of(sellerId),
                items != null ? items : List.of(), Money.of(totalAmount), OrderStatus.fromCode(status),
                PaymentStatus.of(paymentStatus),
                Address.of(address), Phone.of(phone), remark, cancelReason, cancelTime
        );
    }

    // ==================== Status Queries ====================

    /**
     * 是否可支付（仅待付款状态可支付）
     */
    public boolean canPay() {
        return status == OrderStatus.PENDING_PAYMENT;
    }

    /**
     * 是否可取消（仅待付款状态可取消）
     */
    public boolean canCancel() {
        return status == OrderStatus.PENDING_PAYMENT;
    }

    /**
     * 是否可发货（仅已付款状态可发货）
     */
    public boolean canShip() {
        return status == OrderStatus.PAID;
    }

    /**
     * 是否可确认收货（仅已发货状态可确认）
     */
    public boolean canConfirmReceipt() {
        return status == OrderStatus.SHIPPED;
    }

    /**
     * 是否可退款（已付款或已发货状态可退款）
     */
    public boolean canRefund() {
        return status == OrderStatus.PAID || status == OrderStatus.SHIPPED;
    }

    // ==================== State Transitions ====================

    /**
     * 支付订单
     */
    public OrderPaidResult pay() {
        BizRequire.requireTrue(canPay(), OrderResultCode.ORDER_STATUS_ERROR);
        OrderAggregate updated = withStatus(OrderStatus.PAID, PaymentStatus.PAID);
        return new OrderPaidResult(updated, new OrderPaidEvent(id.value(), PaymentStatus.PAID.code()));
    }

    /**
     * 取消订单
     */
    public OrderCancelledResult cancel(String reason) {
        BizRequire.requireTrue(canCancel(), OrderResultCode.ORDER_CANNOT_CANCEL);
        OrderAggregate updated = withStatusAndReason(OrderStatus.CANCELLED, paymentStatus, reason);
        List<Long> productIds = extractProductIds();
        return new OrderCancelledResult(updated, new OrderCancelledEvent(id.value(), productIds, reason));
    }

    /**
     * 发货
     */
    public OrderShippedResult ship() {
        BizRequire.requireTrue(canShip(), OrderResultCode.ORDER_STATUS_ERROR);
        OrderAggregate updated = withStatus(OrderStatus.SHIPPED, paymentStatus);
        return new OrderShippedResult(updated, new OrderShippedEvent(id.value()));
    }

    /**
     * 确认收货
     */
    public OrderCompletedResult confirmReceipt() {
        BizRequire.requireTrue(canConfirmReceipt(), OrderResultCode.ORDER_STATUS_ERROR);
        OrderAggregate updated = withStatus(OrderStatus.COMPLETED, paymentStatus);
        List<Long> productIds = extractProductIds();
        return new OrderCompletedResult(updated, new OrderCompletedEvent(id.value(), productIds));
    }

    /**
     * 退款
     */
    public OrderRefundedResult refund(String reason) {
        BizRequire.requireTrue(canRefund(), OrderResultCode.ORDER_CANNOT_REFUND);
        OrderAggregate updated = withStatusAndReason(OrderStatus.REFUNDED, PaymentStatus.REFUNDED, reason);
        List<Long> productIds = extractProductIds();
        return new OrderRefundedResult(updated, new OrderRefundedEvent(id.value(), productIds, reason));
    }

    // ==================== Internal Helpers ====================

    /**
     * 仅变更订单状态和支付状态，其余字段保持不变，用于安全的不可变状态复制。
     */
    private OrderAggregate withStatus(OrderStatus newStatus, PaymentStatus newPaymentStatus) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, items,
                totalAmount, newStatus, newPaymentStatus,
                address, phone, remark, cancelReason, cancelTime);
    }

    private OrderAggregate withStatusAndReason(OrderStatus newStatus, PaymentStatus newPaymentStatus, String reason) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, items,
                totalAmount, newStatus, newPaymentStatus,
                address, phone, remark, reason, LocalDateTime.now());
    }

    private List<Long> extractProductIds() {
        return items.stream().map(i -> i.productId().value()).toList();
    }

    // ==================== Result Records ====================

    public record OrderCreatedResult(OrderAggregate aggregate, OrderCreatedEvent event) {}
    public record OrderPaidResult(OrderAggregate aggregate, OrderPaidEvent event) {}
    public record OrderCancelledResult(OrderAggregate aggregate, OrderCancelledEvent event) {}
    public record OrderShippedResult(OrderAggregate aggregate, OrderShippedEvent event) {}
    public record OrderCompletedResult(OrderAggregate aggregate, OrderCompletedEvent event) {}
    public record OrderRefundedResult(OrderAggregate aggregate, OrderRefundedEvent event) {}
}
