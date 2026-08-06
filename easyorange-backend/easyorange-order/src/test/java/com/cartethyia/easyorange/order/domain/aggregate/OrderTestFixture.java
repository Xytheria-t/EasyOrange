package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.math.BigDecimal;
import java.util.List;

/**
 * Order 测试夹具 — Test Data Builder 模式 + 快捷工厂方法。
 * <p>
 * 推荐用法：
 * <pre>{@code
 * Order.create(aCreateSpec().build());              // 默认创建参数
 * Order.create(aCreateSpec().items(multiItems()).build());
 * pendingPaymentOrder();                                     // 待付款订单
 * paidOrder();                                               // 已付款订单
 * shippedOrder();                                            // 已发货订单
 * }</pre>
 */
public final class OrderTestFixture {

    public static final String BUYER_ID = "1";
    public static final String SELLER_ID = "2";
    public static final String PRODUCT_ID = "100";
    public static final String ORDER_ID = "ORD1000";
    public static final BigDecimal AMOUNT = new BigDecimal("99.99");

    private OrderTestFixture() {}

    // ==================== Test Data Builder ====================

    public static OrderCreateSpecBuilder aCreateSpec() {
        return new OrderCreateSpecBuilder();
    }

    public static class OrderCreateSpecBuilder {
        private OrderId orderId = OrderId.of(ORDER_ID);
        private UserId buyerId = UserId.of(BUYER_ID);
        private UserId sellerId = UserId.of(SELLER_ID);
        private List<OrderItem> items = singleItemList();
        private Address address = Address.of("北京市朝阳区");
        private Phone phone = Phone.of("13800138000");
        private String remark = "尽快发货";

        public OrderCreateSpecBuilder orderId(String orderId) {
            this.orderId = OrderId.of(orderId);
            return this;
        }

        public OrderCreateSpecBuilder buyerId(String buyerId) {
            this.buyerId = UserId.of(buyerId);
            return this;
        }

        public OrderCreateSpecBuilder sellerId(String sellerId) {
            this.sellerId = UserId.of(sellerId);
            return this;
        }

        public OrderCreateSpecBuilder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public OrderCreateSpecBuilder address(String address) {
            this.address = Address.of(address);
            return this;
        }

        public OrderCreateSpecBuilder phone(String phone) {
            this.phone = Phone.of(phone);
            return this;
        }

        public OrderCreateSpecBuilder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public OrderCreateSpec build() {
            return new OrderCreateSpec(orderId, buyerId, sellerId, items, address, phone, remark);
        }
    }

    // ==================== Reconstruct Builder ====================

    public static OrderReconstructSpecBuilder aReconstructSpec() {
        return new OrderReconstructSpecBuilder();
    }

    public static class OrderReconstructSpecBuilder {
        private OrderId id = OrderId.of("1");
        private OrderNo orderNo = OrderNo.of("ORD1");
        private UserId buyerId = UserId.of(BUYER_ID);
        private UserId sellerId = UserId.of(SELLER_ID);
        private List<OrderItem> items = singleItemList();
        private Money totalAmount = Money.of(AMOUNT);
        private OrderStatus status = OrderStatus.PENDING_PAYMENT;
        private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
        private Address address = Address.of("地址");
        private Phone phone = Phone.of("13800138000");
        private String remark = "备注";
        private String cancelReason = null;
        private java.time.LocalDateTime cancelTime = null;

        public OrderReconstructSpecBuilder id(String id) {
            this.id = OrderId.of(id);
            return this;
        }

        public OrderReconstructSpecBuilder orderNo(String orderNo) {
            this.orderNo = OrderNo.of(orderNo);
            return this;
        }

        public OrderReconstructSpecBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public OrderReconstructSpecBuilder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public OrderReconstructSpecBuilder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public OrderReconstructSpec build() {
            return new OrderReconstructSpec(
                    id,
                    orderNo,
                    buyerId,
                    sellerId,
                    items,
                    totalAmount,
                    status,
                    paymentStatus,
                    address,
                    phone,
                    remark,
                    cancelReason,
                    cancelTime);
        }
    }

    // ==================== Convenience factories ====================

    public static OrderCreateSpec defaultCreateSpec() {
        return aCreateSpec().build();
    }

    public static Order pendingPaymentOrder() {
        return Order.from(aReconstructSpec().status(OrderStatus.PENDING_PAYMENT).build());
    }

    public static Order paidOrder() {
        return Order.from(aReconstructSpec()
                .status(OrderStatus.PAID)
                .paymentStatus(PaymentStatus.PAID)
                .build());
    }

    public static Order shippedOrder() {
        return Order.from(aReconstructSpec()
                .status(OrderStatus.SHIPPED)
                .paymentStatus(PaymentStatus.PAID)
                .build());
    }

    public static Order completedOrder() {
        return Order.from(aReconstructSpec()
                .status(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .build());
    }

    public static Order cancelledOrder() {
        return Order.from(aReconstructSpec()
                .status(OrderStatus.CANCELLED)
                .paymentStatus(PaymentStatus.UNPAID)
                .build());
    }

    public static Order refundedOrder() {
        return Order.from(aReconstructSpec()
                .status(OrderStatus.REFUNDED)
                .paymentStatus(PaymentStatus.REFUNDED)
                .build());
    }

    /**
     * 通过 ID 构建指定状态订单 — 用于 job/saga 测试。
     */
    public static Order orderWithStatus(String orderId, OrderStatus status, PaymentStatus paymentStatus) {
        return Order.from(aReconstructSpec()
                .id(orderId)
                .orderNo("ORD" + orderId)
                .status(status)
                .paymentStatus(paymentStatus)
                .build());
    }

    // ==================== Item helpers ====================

    public static List<OrderItem> singleItemList() {
        return List.of(OrderItem.builder()
                .id("1")
                .productId(ProductId.of(PRODUCT_ID))
                .unitPrice(Money.of(AMOUNT))
                .quantity(1)
                .subtotal(Money.of(AMOUNT))
                .build());
    }

    public static List<OrderItem> multiItemList() {
        return List.of(
                OrderItem.builder()
                        .id("1")
                        .productId(ProductId.of(PRODUCT_ID))
                        .unitPrice(Money.of(AMOUNT))
                        .quantity(1)
                        .subtotal(Money.of(AMOUNT))
                        .build(),
                OrderItem.builder()
                        .id("2")
                        .productId(ProductId.of("200"))
                        .unitPrice(Money.of(new BigDecimal("49.99")))
                        .quantity(2)
                        .subtotal(Money.of(new BigDecimal("99.98")))
                        .build());
    }
}
