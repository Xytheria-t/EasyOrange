package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderAggregate {

    private final OrderId id;
    private final OrderNo orderNo;
    private final UserId buyerId;
    private final UserId sellerId;
    private final ProductId productId;
    private final Money amount;
    private final OrderStatus status;
    private final Integer paymentStatus;
    private final Address address;
    private final Phone phone;
    private final String remark;
    private final String cancelReason;
    private final LocalDateTime cancelTime;

    private OrderAggregate(OrderId id, OrderNo orderNo, UserId buyerId, UserId sellerId, ProductId productId,
                          Money amount, OrderStatus status, Integer paymentStatus,
                          Address address, Phone phone, String remark,
                          String cancelReason, LocalDateTime cancelTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.productId = productId;
        this.amount = amount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.address = address;
        this.phone = phone;
        this.remark = remark;
        this.cancelReason = cancelReason;
        this.cancelTime = cancelTime;
    }

    public OrderId id() { return id; }
    public OrderNo orderNo() { return orderNo; }
    public UserId buyerId() { return buyerId; }
    public UserId sellerId() { return sellerId; }
    public ProductId productId() { return productId; }
    public Money amount() { return amount; }
    public OrderStatus status() { return status; }
    public Integer paymentStatus() { return paymentStatus; }
    public Address address() { return address; }
    public Phone phone() { return phone; }
    public String remark() { return remark; }
    public String cancelReason() { return cancelReason; }
    public LocalDateTime cancelTime() { return cancelTime; }

    public boolean canPay() {
        return status == OrderStatus.PENDING_PAYMENT;
    }

    public boolean canCancel() {
        return status == OrderStatus.PENDING_PAYMENT;
    }

    public boolean canShip() {
        return status == OrderStatus.PAID;
    }

    public boolean canConfirmReceipt() {
        return status == OrderStatus.SHIPPED;
    }

    public boolean canRefund() {
        return status == OrderStatus.PAID || status == OrderStatus.SHIPPED;
    }

    public static OrderCreatedResult createOrder(UserId buyerId, UserId sellerId, ProductId productId,
                                                 Money amount, Address address, Phone phone, String remark) {
        BizRequire.ne(buyerId.value(), sellerId.value(), "不能购买自己的商品");
        BizRequire.notNull(amount, "订单金额不能为空");

        Long orderId = generateOrderId();
        OrderNo orderNo = OrderNo.of(orderId);

        OrderAggregate aggregate = new OrderAggregate(
                OrderId.of(orderId), orderNo, buyerId, sellerId, productId,
                amount, OrderStatus.PENDING_PAYMENT, 0,
                address, phone, remark, null, null
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, buyerId.value(), sellerId.value(), productId.value(), amount.amount()
        );

        return new OrderCreatedResult(aggregate, event);
    }

    public OrderPaidResult pay() {
        BizRequire.requireTrue(canPay(), OrderResultCode.ORDER_STATUS_ERROR);
        OrderAggregate updated = new OrderAggregate(
                id, orderNo, buyerId, sellerId, productId,
                amount, OrderStatus.PAID, 1,
                address, phone, remark, cancelReason, cancelTime
        );
        return new OrderPaidResult(updated, new OrderPaidEvent(updated.id.value(), 1));
    }

    public OrderCancelledResult cancel(String reason) {
        BizRequire.requireTrue(canCancel(), OrderResultCode.ORDER_CANNOT_CANCEL);
        OrderAggregate updated = new OrderAggregate(
                id, orderNo, buyerId, sellerId, productId,
                amount, OrderStatus.CANCELLED, paymentStatus,
                address, phone, remark, reason, LocalDateTime.now()
        );
        return new OrderCancelledResult(updated, new OrderCancelledEvent(updated.id.value(), updated.productId.value(), reason));
    }

    public OrderShippedResult ship() {
        BizRequire.requireTrue(canShip(), OrderResultCode.ORDER_STATUS_ERROR);
        OrderAggregate updated = new OrderAggregate(
                id, orderNo, buyerId, sellerId, productId,
                amount, OrderStatus.SHIPPED, paymentStatus,
                address, phone, remark, cancelReason, cancelTime
        );
        return new OrderShippedResult(updated, new OrderShippedEvent(updated.id.value()));
    }

    public OrderCompletedResult confirmReceipt() {
        BizRequire.requireTrue(canConfirmReceipt(), OrderResultCode.ORDER_STATUS_ERROR);
        OrderAggregate updated = new OrderAggregate(
                id, orderNo, buyerId, sellerId, productId,
                amount, OrderStatus.COMPLETED, paymentStatus,
                address, phone, remark, cancelReason, cancelTime
        );
        return new OrderCompletedResult(updated, new OrderCompletedEvent(updated.id.value(), updated.productId.value()));
    }

    public OrderRefundedResult refund(String reason) {
        BizRequire.requireTrue(canRefund(), OrderResultCode.ORDER_CANNOT_REFUND);
        OrderAggregate updated = new OrderAggregate(
                id, orderNo, buyerId, sellerId, productId,
                amount, OrderStatus.REFUNDED, 2,
                address, phone, remark, reason, LocalDateTime.now()
        );
        return new OrderRefundedResult(updated, new OrderRefundedEvent(updated.id.value(), updated.productId.value(), reason));
    }

    public static OrderAggregate from(OrderId id, OrderNo orderNo, UserId buyerId, UserId sellerId, ProductId productId,
                                      Money amount, OrderStatus status, Integer paymentStatus,
                                      Address address, Phone phone, String remark,
                                      String cancelReason, LocalDateTime cancelTime) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, productId,
                amount, status, paymentStatus, address, phone, remark, cancelReason, cancelTime);
    }

    public static OrderAggregate fromRaw(Long id, String orderNo, Long buyerId, Long sellerId, Long productId,
                                         BigDecimal amount, Integer status, Integer paymentStatus,
                                         String address, String phone, String remark,
                                         String cancelReason, LocalDateTime cancelTime) {
        return new OrderAggregate(
                OrderId.of(id), OrderNo.of(orderNo), UserId.of(buyerId), UserId.of(sellerId), ProductId.of(productId),
                Money.of(amount), OrderStatus.fromCode(status), paymentStatus,
                Address.of(address), Phone.of(phone), remark, cancelReason, cancelTime
        );
    }

    private static Long generateOrderId() {
        return SnowflakeIdGenerator.getInstance().nextId();
    }

    public record OrderCreatedResult(OrderAggregate aggregate, OrderCreatedEvent event) {}
    public record OrderPaidResult(OrderAggregate aggregate, OrderPaidEvent event) {}
    public record OrderCancelledResult(OrderAggregate aggregate, OrderCancelledEvent event) {}
    public record OrderShippedResult(OrderAggregate aggregate, OrderShippedEvent event) {}
    public record OrderCompletedResult(OrderAggregate aggregate, OrderCompletedEvent event) {}
    public record OrderRefundedResult(OrderAggregate aggregate, OrderRefundedEvent event) {}
}
