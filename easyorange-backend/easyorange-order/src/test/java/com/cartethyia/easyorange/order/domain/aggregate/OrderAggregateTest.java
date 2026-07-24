package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderAggregate 单元测试")
class OrderAggregateTest {

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("正常创建订单")
        void createOrder_validParams_returnsResult() {
            var result = OrderAggregate.createOrder(defaultCreateSpec());

            assertThat(result.event().buyerId()).isEqualTo(BUYER_ID);
            assertThat(result.event().sellerId()).isEqualTo(SELLER_ID);
            assertThat(result.event().items()).hasSize(1);
            assertThat(result.event().items().getFirst().productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.event().totalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.event().orderId()).isNotNull();
            assertThat(result.aggregate()).isNotNull();
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(result.aggregate().items()).hasSize(1);
        }

        @Test
        @DisplayName("创建多商品订单")
        void createOrder_multiItem_returnsResult() {
            var result = OrderAggregate.createOrder(
                    aCreateSpec().items(multiItemList()).build());

            assertThat(result.event().items()).hasSize(2);
            assertThat(result.event().totalAmount()).isEqualByComparingTo(new BigDecimal("199.97"));
            assertThat(result.aggregate().totalAmount().value()).isEqualByComparingTo(new BigDecimal("199.97"));
            assertThat(result.aggregate().items()).hasSize(2);
        }

        @Test
        @DisplayName("认领方不能认领自己的资产")
        void createOrder_buyerEqualsSeller_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    aCreateSpec().buyerId(BUYER_ID).sellerId(BUYER_ID).build()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("不能认领自己的资产");
        }

        @Test
        @DisplayName("订单资产不能为空")
        void createOrder_emptyItems_throws() {
            assertThatThrownBy(() -> OrderAggregate.createOrder(
                    aCreateSpec().items(List.of()).build()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("订单资产不能为空");
        }
    }

    @Nested
    @DisplayName("from")
    class FromTests {

        @Test
        @DisplayName("from 正确重建")
        void from_validSpec_returnsAggregate() {
            var aggregate = pendingPaymentOrder();

            assertThat(aggregate.id().value()).isEqualTo("1");
            assertThat(aggregate.orderNo().value()).isEqualTo("ORD1");
            assertThat(aggregate.buyerId().value()).isEqualTo(BUYER_ID);
            assertThat(aggregate.sellerId().value()).isEqualTo(SELLER_ID);
            assertThat(aggregate.items()).hasSize(1);
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
            var aggregate = pendingPaymentOrder();
            var result = aggregate.pay();

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().paymentStatus()).isEqualTo(PaymentStatus.PAID.getCode());
            assertThat(result.aggregate().paymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("已支付状态不能再次支付")
        void pay_alreadyPaid_throws() {
            assertThatThrownBy(paidOrder()::pay).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能支付")
        void pay_cancelled_throws() {
            assertThatThrownBy(cancelledOrder()::pay).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("待付款状态可以取消")
        void cancel_pendingPayment_returnsResultWithEventAndAggregate() {
            var aggregate = pendingPaymentOrder();
            var result = aggregate.cancel("不想要了");

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.event().reason()).isEqualTo("不想要了");
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.aggregate().cancelReason()).isEqualTo("不想要了");
        }

        @Test
        @DisplayName("已发货状态不能取消")
        void cancel_shipped_throws() {
            assertThatThrownBy(() -> shippedOrder().cancel("不想要了"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能再次取消")
        void cancel_alreadyCancelled_throws() {
            assertThatThrownBy(() -> cancelledOrder().cancel("重复取消"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("ship")
    class ShipTests {

        @Test
        @DisplayName("已支付状态可以发货")
        void ship_paid_returnsResultWithEventAndAggregate() {
            var aggregate = paidOrder();
            var result = aggregate.ship();

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("待付款状态不能发货")
        void ship_pendingPayment_throws() {
            assertThatThrownBy(pendingPaymentOrder()::ship).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("confirmReceipt")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("已发货状态可以确认收货")
        void confirmReceipt_shipped_returnsResultWithEventAndAggregate() {
            var aggregate = shippedOrder();
            var result = aggregate.confirmReceipt();

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("待付款状态不能确认收货")
        void confirmReceipt_pendingPayment_throws() {
            assertThatThrownBy(pendingPaymentOrder()::confirmReceipt).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("refund")
    class RefundTests {

        @Test
        @DisplayName("已付款状态可以退款")
        void refund_paid_returnsResultWithEventAndAggregate() {
            var aggregate = paidOrder();
            var result = aggregate.refund("商品有问题");

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.event().reason()).isEqualTo("商品有问题");
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(result.aggregate().paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("已发货状态可以退款")
        void refund_shipped_returnsResultWithEventAndAggregate() {
            var aggregate = shippedOrder();
            var result = aggregate.refund("快递损坏");

            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(result.event().reason()).isEqualTo("快递损坏");
        }

        @Test
        @DisplayName("待付款状态不能退款")
        void refund_pendingPayment_throws() {
            assertThatThrownBy(() -> pendingPaymentOrder().refund("测试"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已完成状态不能退款")
        void refund_completed_throws() {
            assertThatThrownBy(() -> completedOrder().refund("测试"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("已取消状态不能退款")
        void refund_cancelled_throws() {
            assertThatThrownBy(() -> cancelledOrder().refund("测试"))
                    .isInstanceOf(Exception.class);
        }
    }
}
