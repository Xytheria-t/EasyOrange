package com.cartethyia.easyorange.order.domain.aggregate;

import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Order 单元测试")
class OrderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 31, 10, 0);

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("正常创建订单")
        void createOrder_validParams_returnsResult() {
            var result = Order.createOrder(defaultCreateSpec());

            assertThat(result.event().buyerId()).isEqualTo(BUYER_ID);
            assertThat(result.event().sellerId()).isEqualTo(SELLER_ID);
            assertThat(result.event().items()).hasSize(1);
            assertThat(result.event().items().getFirst().productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.event().totalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.event().orderId()).isNotNull();
            assertThat(result.aggregate()).isNotNull();
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(result.aggregate().items()).hasSize(1);
            assertThat(result.aggregate().version()).isEqualTo(Version.INITIAL);
        }

        @Test
        @DisplayName("创建多商品订单")
        void createOrder_multiItem_returnsResult() {
            var result = Order.createOrder(aCreateSpec().items(multiItemList()).build());

            assertThat(result.event().items()).hasSize(2);
            assertThat(result.event().totalAmount()).isEqualByComparingTo(new BigDecimal("199.97"));
            assertThat(result.aggregate().totalAmount().value()).isEqualByComparingTo(new BigDecimal("199.97"));
            assertThat(result.aggregate().items()).hasSize(2);
        }

        @Test
        @DisplayName("认领方不能认领自己的资产")
        void createOrder_buyerEqualsSeller_throws() {
            assertThatThrownBy(() -> Order.createOrder(
                            aCreateSpec().buyerId(BUYER_ID).sellerId(BUYER_ID).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能认领自己的资产");
        }

        @Test
        @DisplayName("订单资产不能为空")
        void createOrder_emptyItems_throws() {
            assertThatThrownBy(() ->
                            Order.createOrder(aCreateSpec().items(List.of()).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单资产不能为空");
        }
    }

    @Nested
    @DisplayName("from")
    class FromTests {

        @Test
        @DisplayName("from 正确重建")
        void from_validSpec_returnsAggregate() {
            var aggregate = Order.from(aReconstructSpec().version(Version.of(5)).build());

            assertThat(aggregate.id().value()).isEqualTo("1");
            assertThat(aggregate.orderNo().value()).isEqualTo("ORD1");
            assertThat(aggregate.buyerId().value()).isEqualTo(BUYER_ID);
            assertThat(aggregate.sellerId().value()).isEqualTo(SELLER_ID);
            assertThat(aggregate.items()).hasSize(1);
            assertThat(aggregate.totalAmount().value()).isEqualByComparingTo(AMOUNT);
            assertThat(aggregate.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(aggregate.version().value()).isEqualTo(5);
        }

        @Test
        @DisplayName("状态转换保留乐观锁版本号")
        void transition_preservesVersion() {
            var aggregate = pendingPaymentOrder();

            var paid = aggregate.pay(LocalDateTime.now()).aggregate();

            assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
            assertThat(paid.version()).isEqualTo(aggregate.version());
        }
    }

    @Nested
    @DisplayName("pay")
    class PayTests {

        @Test
        @DisplayName("待付款状态可以支付")
        void pay_pendingPayment_returnsResultWithEventAndAggregate() {
            var aggregate = pendingPaymentOrder();
            var result = aggregate.pay(NOW);

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().paymentStatus()).isEqualTo(PaymentStatus.PAID.getCode());
            assertThat(result.aggregate().paymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.PAID);
        }

        @ParameterizedTest(name = "[{index}] {0}状态不能支付")
        @MethodSource("com.cartethyia.easyorange.order.domain.aggregate.OrderTest#nonPayableOrders")
        @DisplayName("非待付款状态不能支付")
        void pay_nonPayable_throwsBusinessException(String stateName, Order aggregate) {
            assertThatThrownBy(() -> aggregate.pay(NOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_STATUS_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("待付款状态可以取消")
        void cancel_pendingPayment_returnsResultWithEventAndAggregate() {
            var aggregate = pendingPaymentOrder();
            var result = aggregate.cancel("不想要了", NOW);

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.event().reason()).isEqualTo("不想要了");
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.aggregate().cancelReason()).isEqualTo("不想要了");
            assertThat(result.aggregate().refundReason()).isNull();
        }

        @ParameterizedTest(name = "[{index}] {0}状态不能取消")
        @MethodSource("com.cartethyia.easyorange.order.domain.aggregate.OrderTest#nonCancellableOrders")
        @DisplayName("非待付款状态不能取消")
        void cancel_nonCancellable_throwsBusinessException(String stateName, Order aggregate) {
            assertThatThrownBy(() -> aggregate.cancel("不想要了", NOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_CANNOT_CANCEL.getCode());
        }
    }

    @Nested
    @DisplayName("ship")
    class ShipTests {

        @Test
        @DisplayName("已支付状态可以发货")
        void ship_paid_returnsResultWithEventAndAggregate() {
            var aggregate = paidOrder();
            var result = aggregate.ship(NOW);

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("待付款状态不能发货")
        void ship_pendingPayment_throwsBusinessException() {
            assertThatThrownBy(() -> pendingPaymentOrder().ship(NOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_STATUS_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("confirmReceipt")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("已发货状态可以确认收货")
        void confirmReceipt_shipped_returnsResultWithEventAndAggregate() {
            var aggregate = shippedOrder();
            var result = aggregate.confirmReceipt(NOW);

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("待付款状态不能确认收货")
        void confirmReceipt_pendingPayment_throwsBusinessException() {
            assertThatThrownBy(() -> pendingPaymentOrder().confirmReceipt(NOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_STATUS_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("refund")
    class RefundTests {

        @Test
        @DisplayName("已付款状态可以退款")
        void refund_paid_returnsResultWithEventAndAggregate() {
            var aggregate = paidOrder();
            var result = aggregate.refund("商品有问题", NOW);

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
            var result = aggregate.refund("快递损坏", NOW);

            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(result.event().reason()).isEqualTo("快递损坏");
        }

        @Test
        @DisplayName("退款原因记入 refundReason，不污染 cancelReason")
        void refund_recordsReasonIntoRefundFields_only() {
            var result = paidOrder().refund("商品有问题", NOW);

            assertThat(result.aggregate().refundReason()).isEqualTo("商品有问题");
            assertThat(result.aggregate().refundTime()).isNotNull();
            assertThat(result.aggregate().cancelReason()).isNull();
            assertThat(result.aggregate().cancelTime()).isNull();
        }

        @ParameterizedTest(name = "[{index}] {0}状态不能退款")
        @MethodSource("com.cartethyia.easyorange.order.domain.aggregate.OrderTest#nonRefundableOrders")
        @DisplayName("不可退款状态抛业务异常")
        void refund_nonRefundable_throwsBusinessException(String stateName, Order aggregate) {
            assertThatThrownBy(() -> aggregate.refund("测试", NOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_CANNOT_REFUND.getCode());
        }
    }

    @Nested
    @DisplayName("forceCancel")
    class ForceCancelTests {

        @Test
        @DisplayName("待付款订单可以强制取消")
        void forceCancel_pendingPayment_returnsResult() {
            var aggregate = pendingPaymentOrder();
            var result = aggregate.forceCancel("管理端操作", NOW);

            assertThat(result.event().orderId()).isEqualTo(aggregate.id().value());
            assertThat(result.event().productIds()).containsExactly(PRODUCT_ID);
            assertThat(result.event().reason()).isEqualTo("管理端操作");
            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.aggregate().cancelReason()).isEqualTo("管理端操作");
        }

        @Test
        @DisplayName("已付款订单可以强制取消")
        void forceCancel_paid_returnsResult() {
            var result = paidOrder().forceCancel("管理端操作", NOW);

            assertThat(result.aggregate().status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.aggregate().cancelReason()).isEqualTo("管理端操作");
        }

        @ParameterizedTest(name = "[{index}] {0}状态不能强制取消")
        @MethodSource("com.cartethyia.easyorange.order.domain.aggregate.OrderTest#nonForceCancellableOrders")
        @DisplayName("非法状态强制取消抛业务异常")
        void forceCancel_invalidState_throwsBusinessException(String stateName, Order aggregate) {
            assertThatThrownBy(() -> aggregate.forceCancel("管理端操作", NOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(OrderResultCode.ORDER_STATUS_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("取消时间")
    class CancelTimeTests {

        @Test
        @DisplayName("cancel 使用调用方传入的时间")
        void cancel_usesProvidedTime() {
            var aggregate = pendingPaymentOrder();

            var result = aggregate.cancel("不想要了", NOW);

            assertThat(result.aggregate().cancelTime()).isEqualTo(NOW);
        }
    }

    // ==================== Parameterized fixtures ====================

    /** pay() 的非法前置状态 — 已支付 / 已取消 */
    static Stream<Arguments> nonPayableOrders() {
        return Stream.of(Arguments.of("已支付", paidOrder()), Arguments.of("已取消", cancelledOrder()));
    }

    /** cancel() 的非法前置状态 — 已发货 / 已取消 */
    static Stream<Arguments> nonCancellableOrders() {
        return Stream.of(Arguments.of("已发货", shippedOrder()), Arguments.of("已取消", cancelledOrder()));
    }

    /** refund() 的非法前置状态 — 待付款 / 已完成 / 已取消 */
    static Stream<Arguments> nonRefundableOrders() {
        return Stream.of(
                Arguments.of("待付款", pendingPaymentOrder()),
                Arguments.of("已完成", completedOrder()),
                Arguments.of("已取消", cancelledOrder()));
    }

    /** forceCancel() 的非法前置状态 — 已发货 / 已完成 / 已退款 */
    static Stream<Arguments> nonForceCancellableOrders() {
        return Stream.of(
                Arguments.of("已发货", shippedOrder()),
                Arguments.of("已完成", completedOrder()),
                Arguments.of("已退款", refundedOrder()));
    }
}
