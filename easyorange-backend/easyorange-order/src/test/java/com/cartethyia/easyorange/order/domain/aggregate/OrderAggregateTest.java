package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderAggregate 单元测试")
class OrderAggregateTest {

    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "100";
    private static final String ORDER_ID = "ORD1000";
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");

    private static List<OrderItem> singleItemList() {
        return List.of(OrderItem.builder()
                .id("1")
                .productId(ProductId.of(PRODUCT_ID))
                .unitPrice(Money.of(AMOUNT))
                .quantity(1)
                .subtotal(Money.of(AMOUNT))
                .build());
    }

    private static List<OrderItem> multiItemList() {
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
                        .build()
        );
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("正常创建订单")
        void createOrder_validParams_returnsResult() {
            OrderAggregate.OrderCreatedResult result = OrderAggregate.createOrder(
                    UserId.of(BUYER_ID), UserId.of(SELLER_ID), singleItemList(),
                    Address.of("北京市朝阳区"), Phone.of("13800138000"), "尽快发货",
                    ORDER_ID
            );

            assertThat(result.event().buyerId()).isEqualTo(BUYER_ID);
            assertThat(result.event().sellerId()).isEqualTo(SELLER_ID);
            assertThat(result.event().items()).hasSize(1);
            assertThat(result.event().items().get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.event().totalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.event().orderId()).isNotNull();
            assertThat(result.aggregate()).isNotNull();
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(result.aggregate().items()).hasSize(1);
        }

        @Test
        @DisplayName("创建多商品订单")
        void createOrder_multiItem_returnsResult() {
            OrderAggregate.OrderCreatedResult result = OrderAggregate.createOrder(
                    UserId.of(BUYER_ID), UserId.of(SELLER_ID), multiItemList(),
                    Address.of("北京市朝阳区"), Phone.of("13800138000"), "尽快发货",
                    ORDER_ID
            );

            assertThat(result.event().items()).hasSize(2);
            assertThat(result.event().totalAmount()).isEqualByComparingTo(new BigDecimal("199.97"));
            assertThat(result.aggregate().totalAmount().value()).isEqualByComparingTo(new BigDecimal("199.97"));
            assertThat(result.aggregate().items()).hasSize(2);
        }

        @Test
        @DisplayName("认领方不能认领自己的资产")
        void createOrder_buyerEqualsSeller_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    UserId.of(BUYER_ID), UserId.of(BUYER_ID), singleItemList(),
                    Address.of("地址"), Phone.of("13800138000"), "备注",
                    ORDER_ID
            )).isInstanceOf(Exception.class)
              .hasMessageContaining("不能认领自己的资产");
        }

        @Test
        @DisplayName("订单金额必须大于0")
        void createOrder_zeroAmount_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    UserId.of(BUYER_ID), UserId.of(SELLER_ID), List.of(),
                    Address.of("地址"), Phone.of("13800138000"), "备注",
                    ORDER_ID
            )).isInstanceOf(Exception.class)
              .hasMessageContaining("订单资产不能为空");
        }
    }

    @Nested
    @DisplayName("fromRaw")
    class FromRawTests {

        @Test
        @DisplayName("fromRaw 正确转换")
        void fromRaw_validParams_returnsAggregate() {
            OrderAggregate aggregate = OrderAggregate.fromRaw(
                    "1", "ORD123", BUYER_ID, SELLER_ID,
                    AMOUNT, 0, 0, "地址", "13800138000", "备注", null, null
            );

            assertThat(aggregate.id().value()).isEqualTo("1");
            assertThat(aggregate.orderNo().value()).isEqualTo("ORD123");
            assertThat(aggregate.buyerId().value()).isEqualTo(BUYER_ID);
            assertThat(aggregate.sellerId().value()).isEqualTo(SELLER_ID);
            assertThat(aggregate.items()).isEmpty();
            assertThat(aggregate.totalAmount().value()).isEqualByComparingTo(AMOUNT);
            assertThat(aggregate.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        }
    }

    @Nested
    @DisplayName("pay")
    class PayTests {

        @Test
        @DisplayName("待付款状态可以支付")
        void pay_pendingPayment_returnsResultWithEventAndAggregate() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            OrderAggregate.OrderPaidResult result = aggregate.pay();

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().paymentStatus()).isEqualTo(1);
            assertThat(result.aggregate().paymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("已支付状态不能再次支付")
        void pay_alreadyPaid_throws() {
            OrderAggregate aggregate = createPaidAggregate();
            assertThatThrownBy(aggregate::pay)
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能支付")
        void pay_cancelled_throws() {
            OrderAggregate aggregate = createCancelledAggregate();
            assertThatThrownBy(aggregate::pay)
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("待付款状态可以取消")
        void cancel_pendingPayment_returnsResultWithEventAndAggregate() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            OrderAggregate.OrderCancelledResult result = aggregate.cancel("不想要了");

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.event().reason()).isEqualTo("不想要了");
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.aggregate().cancelReason()).isEqualTo("不想要了");
        }

        @Test
        @DisplayName("已发货状态不能取消")
        void cancel_shipped_throws() {
            OrderAggregate aggregate = createShippedAggregate();
            assertThatThrownBy(() -> aggregate.cancel("不想要了"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能再次取消")
        void cancel_alreadyCancelled_throws() {
            OrderAggregate aggregate = createCancelledAggregate();
            assertThatThrownBy(() -> aggregate.cancel("重复取消"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("ship")
    class ShipTests {

        @Test
        @DisplayName("已支付状态可以发货")
        void ship_paid_returnsResultWithEventAndAggregate() {
            OrderAggregate aggregate = createPaidAggregate();
            OrderAggregate.OrderShippedResult result = aggregate.ship();

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("待付款状态不能发货")
        void ship_pendingPayment_throws() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            assertThatThrownBy(aggregate::ship)
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("confirmReceipt")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("已发货状态可以确认收货")
        void confirmReceipt_shipped_returnsResultWithEventAndAggregate() {
            OrderAggregate aggregate = createShippedAggregate();
            OrderAggregate.OrderCompletedResult result = aggregate.confirmReceipt();

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("待付款状态不能确认收货")
        void confirmReceipt_pendingPayment_throws() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            assertThatThrownBy(aggregate::confirmReceipt)
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("refund")
    class RefundTests {

        @Test
        @DisplayName("已付款状态可以退款")
        void refund_paid_returnsResultWithEventAndAggregate() {
            OrderAggregate aggregate = createPaidAggregate();
            OrderAggregate.OrderRefundedResult result = aggregate.refund("商品有问题");

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.event().reason()).isEqualTo("商品有问题");
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(result.aggregate().paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("已发货状态可以退款")
        void refund_shipped_returnsResultWithEventAndAggregate() {
            OrderAggregate aggregate = createShippedAggregate();
            OrderAggregate.OrderRefundedResult result = aggregate.refund("快递损坏");

            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(result.event().reason()).isEqualTo("快递损坏");
        }

        @Test
        @DisplayName("待付款状态不能退款")
        void refund_pendingPayment_throws() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            assertThatThrownBy(() -> aggregate.refund("测试"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已完成状态不能退款")
        void refund_completed_throws() {
            OrderAggregate aggregate = createCompletedAggregate();
            assertThatThrownBy(() -> aggregate.refund("测试"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能退款")
        void refund_cancelled_throws() {
            OrderAggregate aggregate = createCancelledAggregate();
            assertThatThrownBy(() -> aggregate.refund("测试"))
                    .isInstanceOf(Exception.class);
        }
    }

    private static List<OrderItem> itemForTest() {
        return List.of(OrderItem.builder()
                .id("1")
                .productId(ProductId.of(PRODUCT_ID))
                .unitPrice(Money.of(AMOUNT))
                .quantity(1)
                .subtotal(Money.of(AMOUNT))
                .build());
    }

    private OrderAggregate createPendingPaymentAggregate() {
        return OrderAggregate.from(
                OrderId.of("1"), OrderNo.of("ORD1"),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
                Money.of(AMOUNT), OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID,
                Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createPaidAggregate() {
        return OrderAggregate.from(
                OrderId.of("1"), OrderNo.of("ORD1"),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
                Money.of(AMOUNT), OrderStatus.PAID, PaymentStatus.PAID,
                Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createShippedAggregate() {
        return OrderAggregate.from(
                OrderId.of("1"), OrderNo.of("ORD1"),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
                Money.of(AMOUNT), OrderStatus.SHIPPED, PaymentStatus.PAID,
                Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createCompletedAggregate() {
        return OrderAggregate.from(
                OrderId.of("1"), OrderNo.of("ORD1"),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
                Money.of(AMOUNT), OrderStatus.COMPLETED, PaymentStatus.PAID,
                Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }

    private OrderAggregate createCancelledAggregate() {
        return OrderAggregate.from(
                OrderId.of("1"), OrderNo.of("ORD1"),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
                Money.of(AMOUNT), OrderStatus.CANCELLED, PaymentStatus.UNPAID,
                Address.of("地址"), Phone.of("13800138000"), "备注", null, null
        );
    }
}
