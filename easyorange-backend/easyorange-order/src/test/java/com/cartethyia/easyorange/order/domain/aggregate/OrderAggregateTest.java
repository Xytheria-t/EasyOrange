package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderAggregate 单元测试")
class OrderAggregateTest {

    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final Long PRODUCT_ID = 100L;
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("正常创建订单事件")
        void createOrder_validParams_returnsEvent() {
            OrderCreatedEvent event = OrderAggregate.createOrder(
                    BUYER_ID, SELLER_ID, PRODUCT_ID, AMOUNT,
                    "北京市朝阳区", "13800138000", "尽快发货"
            );

            assertThat(event.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(event.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(event.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(event.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(event.getOrderId()).isNotNull();
        }

        @Test
        @DisplayName("买家不能购买自己的商品")
        void createOrder_buyerEqualsSeller_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    BUYER_ID, BUYER_ID, PRODUCT_ID, AMOUNT,
                    "地址", "电话", "备注"
            )).isInstanceOf(Exception.class)
              .hasMessageContaining("不能购买自己的商品");
        }

        @Test
        @DisplayName("订单金额必须大于0")
        void createOrder_zeroAmount_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    BUYER_ID, SELLER_ID, PRODUCT_ID, BigDecimal.ZERO,
                    "地址", "电话", "备注"
            )).isInstanceOf(Exception.class)
              .hasMessageContaining("订单金额必须大于0");
        }

        @Test
        @DisplayName("订单金额为负数时抛出异常")
        void createOrder_negativeAmount_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    BUYER_ID, SELLER_ID, PRODUCT_ID, new BigDecimal("-10"),
                    "地址", "电话", "备注"
            )).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("fromEntity / toEntity")
    class EntityConversionTests {

        @Test
        @DisplayName("fromEntity 正确转换")
        void fromEntity_validOrder_returnsAggregate() {
            Order order = Order.builder()
                    .id(1L).orderNo("ORD123")
                    .buyerId(BUYER_ID).sellerId(SELLER_ID)
                    .productId(PRODUCT_ID).amount(AMOUNT)
                    .status(0).paymentStatus(0)
                    .address("地址").phone("电话").remark("备注")
                    .build();

            OrderAggregate aggregate = OrderAggregate.fromEntity(order);

            assertThat(aggregate.getId()).isEqualTo(1L);
            assertThat(aggregate.getOrderNo()).isEqualTo("ORD123");
            assertThat(aggregate.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(aggregate.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(aggregate.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(aggregate.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(aggregate.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("fromEntity 返回 null 对于 null 输入")
        void fromEntity_null_returnsNull() {
            assertThat(OrderAggregate.fromEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity 正确转换")
        void toEntity_returnsOrderWithAllFields() {
            OrderAggregate aggregate = OrderAggregate.from(
                    1L, "ORD123", BUYER_ID, SELLER_ID, PRODUCT_ID,
                    AMOUNT, 0, 0, "地址", "电话", "备注", null, null
            );

            Order order = aggregate.toEntity();

            assertThat(order.getId()).isEqualTo(1L);
            assertThat(order.getOrderNo()).isEqualTo("ORD123");
            assertThat(order.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(order.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(order.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(order.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(order.getStatus()).isEqualTo(0);
            assertThat(order.getPaymentStatus()).isEqualTo(0);
            assertThat(order.getAddress()).isEqualTo("地址");
            assertThat(order.getPhone()).isEqualTo("电话");
            assertThat(order.getRemark()).isEqualTo("备注");
        }
    }

    @Nested
    @DisplayName("pay")
    class PayTests {

        @Test
        @DisplayName("待付款状态可以支付")
        void pay_pendingPayment_returnsEvent() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            OrderPaidEvent event = aggregate.pay();
            assertThat(event.getOrderId()).isEqualTo(aggregate.getId());
            assertThat(event.getPaymentStatus()).isEqualTo(1);
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

        @Test
        @DisplayName("已发货状态不能支付")
        void pay_shipped_throws() {
            OrderAggregate aggregate = createShippedAggregate();
            assertThatThrownBy(aggregate::pay)
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("待付款状态可以取消")
        void cancel_pendingPayment_returnsEvent() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            OrderCancelledEvent event = aggregate.cancel("不想要了");
            assertThat(event.getOrderId()).isEqualTo(aggregate.getId());
            assertThat(event.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(event.getReason()).isEqualTo("不想要了");
        }

        @Test
        @DisplayName("已发货状态不能取消")
        void cancel_shipped_throws() {
            OrderAggregate aggregate = createShippedAggregate();
            assertThatThrownBy(() -> aggregate.cancel("不想要了"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已完成状态不能取消")
        void cancel_completed_throws() {
            OrderAggregate aggregate = createCompletedAggregate();
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
        void ship_paid_returnsEvent() {
            OrderAggregate aggregate = createPaidAggregate();
            OrderShippedEvent event = aggregate.ship();
            assertThat(event.getOrderId()).isEqualTo(aggregate.getId());
        }

        @Test
        @DisplayName("待付款状态不能发货")
        void ship_pendingPayment_throws() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            assertThatThrownBy(aggregate::ship)
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已发货状态不能再次发货")
        void ship_alreadyShipped_throws() {
            OrderAggregate aggregate = createShippedAggregate();
            assertThatThrownBy(aggregate::ship)
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能发货")
        void ship_cancelled_throws() {
            OrderAggregate aggregate = createCancelledAggregate();
            assertThatThrownBy(aggregate::ship)
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("confirmReceipt")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("已发货状态可以确认收货")
        void confirmReceipt_shipped_returnsEvent() {
            OrderAggregate aggregate = createShippedAggregate();
            OrderCompletedEvent event = aggregate.confirmReceipt();
            assertThat(event.getOrderId()).isEqualTo(aggregate.getId());
            assertThat(event.getProductId()).isEqualTo(PRODUCT_ID);
        }

        @Test
        @DisplayName("待付款状态不能确认收货")
        void confirmReceipt_pendingPayment_throws() {
            OrderAggregate aggregate = createPendingPaymentAggregate();
            assertThatThrownBy(aggregate::confirmReceipt)
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已支付状态不能确认收货")
        void confirmReceipt_paid_throws() {
            OrderAggregate aggregate = createPaidAggregate();
            assertThatThrownBy(aggregate::confirmReceipt)
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已完成状态不能再次确认收货")
        void confirmReceipt_alreadyCompleted_throws() {
            OrderAggregate aggregate = createCompletedAggregate();
            assertThatThrownBy(aggregate::confirmReceipt)
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能确认收货")
        void confirmReceipt_cancelled_throws() {
            OrderAggregate aggregate = createCancelledAggregate();
            assertThatThrownBy(aggregate::confirmReceipt)
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("withXxx 不可变方法")
    class WithMethodsTests {

        @Test
        @DisplayName("withStatus 返回新实例，原实例不变")
        void withStatus_returnsNewInstance() {
            OrderAggregate original = createPendingPaymentAggregate();
            OrderAggregate updated = original.withStatus(OrderStatus.PAID.getCode());

            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID.getCode());
            assertThat(original.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT.getCode());
        }

        @Test
        @DisplayName("withPaymentStatus 返回新实例")
        void withPaymentStatus_returnsNewInstance() {
            OrderAggregate original = createPendingPaymentAggregate();
            OrderAggregate updated = original.withPaymentStatus(1);

            assertThat(updated.getPaymentStatus()).isEqualTo(1);
            assertThat(original.getPaymentStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("withId 返回新实例")
        void withId_returnsNewInstance() {
            OrderAggregate original = createPendingPaymentAggregate();
            OrderAggregate updated = original.withId(999L);

            assertThat(updated.getId()).isEqualTo(999L);
            assertThat(original.getId()).isEqualTo(1L);
        }
    }

    private OrderAggregate createPendingPaymentAggregate() {
        return OrderAggregate.from(
                1L, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                AMOUNT, OrderStatus.PENDING_PAYMENT.getCode(), 0,
                "地址", "电话", "备注", null, null
        );
    }

    private OrderAggregate createPaidAggregate() {
        return OrderAggregate.from(
                1L, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                AMOUNT, OrderStatus.PAID.getCode(), 1,
                "地址", "电话", "备注", null, null
        );
    }

    private OrderAggregate createShippedAggregate() {
        return OrderAggregate.from(
                1L, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                AMOUNT, OrderStatus.SHIPPED.getCode(), 1,
                "地址", "电话", "备注", null, null
        );
    }

    private OrderAggregate createCompletedAggregate() {
        return OrderAggregate.from(
                1L, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                AMOUNT, OrderStatus.COMPLETED.getCode(), 1,
                "地址", "电话", "备注", null, null
        );
    }

    private OrderAggregate createCancelledAggregate() {
        return OrderAggregate.from(
                1L, "ORD1", BUYER_ID, SELLER_ID, PRODUCT_ID,
                AMOUNT, OrderStatus.CANCELLED.getCode(), 0,
                "地址", "电话", "备注", null, null
        );
    }
}
