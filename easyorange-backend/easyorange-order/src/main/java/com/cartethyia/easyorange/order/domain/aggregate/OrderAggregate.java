package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderAggregate {

    private final Long id;
    private final String orderNo;
    private final Long buyerId;
    private final Long sellerId;
    private final Long productId;
    private final BigDecimal amount;
    private final Integer status;
    private final Integer paymentStatus;
    private final String address;
    private final String phone;
    private final String remark;
    private final String cancelReason;
    private final java.time.LocalDateTime cancelTime;

    private OrderAggregate(Long id, String orderNo, Long buyerId, Long sellerId, Long productId,
                          BigDecimal amount, Integer status, Integer paymentStatus,
                          String address, String phone, String remark,
                          String cancelReason, java.time.LocalDateTime cancelTime) {
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

    public static OrderCreatedEvent createOrder(Long buyerId, Long sellerId, Long productId,
                                               BigDecimal amount, String address, String phone,
                                               String remark) {
        BizRequire.ne(buyerId, sellerId, "不能购买自己的商品");
        BizRequire.notNull(amount, "订单金额不能为空");
        BizRequire.requireTrue(amount.compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");

        Long orderId = generateOrderId();
        String orderNo = generateOrderNo();

        return new OrderCreatedEvent(orderId, buyerId, sellerId, productId, amount);
    }

    public OrderAggregate withId(Long id) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, productId,
                amount, status, paymentStatus, address, phone, remark, cancelReason, cancelTime);
    }

    public OrderAggregate withOrderNo(String orderNo) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, productId,
                amount, status, paymentStatus, address, phone, remark, cancelReason, cancelTime);
    }

    public OrderAggregate withStatus(Integer status) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, productId,
                amount, status, paymentStatus, address, phone, remark, cancelReason, cancelTime);
    }

    public OrderAggregate withPaymentStatus(Integer paymentStatus) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, productId,
                amount, status, paymentStatus, address, phone, remark, cancelReason, cancelTime);
    }

    public OrderPaidEvent pay() {
        BizRequire.requireTrue(OrderStatus.canPay(this.status), OrderResultCode.ORDER_STATUS_ERROR);
        return new OrderPaidEvent(this.id, 1);
    }

    public OrderCancelledEvent cancel(String reason) {
        BizRequire.requireTrue(OrderStatus.canCancel(this.status), OrderResultCode.ORDER_CANNOT_CANCEL);
        return new OrderCancelledEvent(this.id, this.productId, reason);
    }

    public OrderShippedEvent ship() {
        BizRequire.requireTrue(OrderStatus.canShip(this.status), OrderResultCode.ORDER_STATUS_ERROR);
        return new OrderShippedEvent(this.id);
    }

    public OrderCompletedEvent confirmReceipt() {
        BizRequire.requireTrue(OrderStatus.canConfirmReceipt(this.status), OrderResultCode.ORDER_STATUS_ERROR);
        return new OrderCompletedEvent(this.id, this.productId);
    }

    public static OrderAggregate from(Long id, String orderNo, Long buyerId, Long sellerId, Long productId,
                                     BigDecimal amount, Integer status, Integer paymentStatus,
                                     String address, String phone, String remark,
                                     String cancelReason, java.time.LocalDateTime cancelTime) {
        return new OrderAggregate(id, orderNo, buyerId, sellerId, productId,
                amount, status, paymentStatus, address, phone, remark, cancelReason, cancelTime);
    }

    public static OrderAggregate fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderAggregate(
                order.getId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getProductId(),
                order.getAmount(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getAddress(),
                order.getPhone(),
                order.getRemark(),
                order.getCancelReason(),
                order.getCancelTime()
        );
    }

    public Order toEntity() {
        return Order.builder()
                .id(this.id)
                .orderNo(this.orderNo)
                .buyerId(this.buyerId)
                .sellerId(this.sellerId)
                .productId(this.productId)
                .amount(this.amount)
                .status(this.status)
                .paymentStatus(this.paymentStatus)
                .address(this.address)
                .phone(this.phone)
                .remark(this.remark)
                .cancelReason(this.cancelReason)
                .cancelTime(this.cancelTime)
                .build();
    }

    private static Long generateOrderId() {
        return System.currentTimeMillis();
    }

    private static String generateOrderNo() {
        return "ORD" + System.currentTimeMillis();
    }
}
