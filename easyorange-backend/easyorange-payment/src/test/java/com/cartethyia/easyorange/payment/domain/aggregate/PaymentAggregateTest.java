package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentGateway;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentResult;
import com.cartethyia.easyorange.payment.domain.gateway.RefundResult;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentAggregate 聚合根测试")
class PaymentAggregateTest {

    private final PaymentGateway mockGateway = new PaymentGateway() {
        @Override
        public PaymentResult pay(PaymentAggregate aggregate) {
            return PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis());
        }

        @Override
        public RefundResult refund(PaymentAggregate aggregate, BigDecimal refundAmount) {
            return RefundResult.success("MOCK_REF_" + System.currentTimeMillis());
        }
    };

    @Nested
    @DisplayName("create 静态工厂方法")
    class CreateTests {

        @Test
        @DisplayName("创建支付成功")
        void create_withValidParams_createsPayment() {
            PaymentAggregate aggregate = PaymentAggregate.create(
                1001L, 2001L, new BigDecimal("99.99"), 1, "attach_data"
            );

            assertThat(aggregate.id()).isNotNull();
            assertThat(aggregate.paymentNo()).startsWith("PAY");
            assertThat(aggregate.orderId()).isEqualTo(1001L);
            assertThat(aggregate.userId()).isEqualTo(2001L);
            assertThat(aggregate.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(aggregate.paymentMethod()).isEqualTo(1);
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("create 返回的聚合根可通过 publishCreatedEvent 发布事件")
        void create_publishCreatedEvent() {
            PaymentAggregate aggregate = PaymentAggregate.create(
                1001L, 2001L, new BigDecimal("99.99"), 1, "attach_data"
            );

            PaymentCreatedEvent event = PaymentAggregate.publishCreatedEvent(aggregate);

            assertThat(event.getPaymentId()).isEqualTo(aggregate.id());
            assertThat(event.getPaymentNo()).isEqualTo(aggregate.paymentNo());
            assertThat(event.getOrderId()).isEqualTo(1001L);
            assertThat(event.getUserId()).isEqualTo(2001L);
            assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(event.getPaymentMethod()).isEqualTo(1);
        }

        @Test
        @DisplayName("orderId 为空抛出异常")
        void create_withNullOrderId_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                null, 2001L, new BigDecimal("99.99"), 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("订单ID不能为空");
        }

        @Test
        @DisplayName("userId 为空抛出异常")
        void create_withNullUserId_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, null, new BigDecimal("99.99"), 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("用户ID不能为空");
        }

        @Test
        @DisplayName("amount 为空抛出异常")
        void create_withNullAmount_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, 2001L, null, 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付金额不能为空");
        }

        @Test
        @DisplayName("amount 小于等于零抛出异常")
        void create_withZeroAmount_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, 2001L, BigDecimal.ZERO, 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付金额必须大于0");
        }

        @Test
        @DisplayName("paymentMethod 为空抛出异常")
        void create_withNullPaymentMethod_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, 2001L, new BigDecimal("99.99"), null, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付方式不能为空");
        }
    }

    @Nested
    @DisplayName("reconstruct 重建方法")
    class ReconstructTests {

        @Test
        @DisplayName("reconstruct 正确重建聚合根")
        void reconstruct_convertsCorrectly() {
            PaymentAggregate aggregate = PaymentAggregate.reconstruct(
                1001L, "PAY123456", 2001L, 3001L,
                new BigDecimal("99.99"), BigDecimal.ZERO, 1,
                PaymentStatus.SUCCESS, "TXN123", "已退款",
                LocalDateTime.now(), "attach",
                LocalDateTime.now(), LocalDateTime.now()
            );

            assertThat(aggregate.id()).isEqualTo(1001L);
            assertThat(aggregate.paymentNo()).isEqualTo("PAY123456");
            assertThat(aggregate.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("pay 支付方法")
    class PayTests {

        @Test
        @DisplayName("待支付状态支付成功")
        void pay_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            PaymentSucceededEvent event = (PaymentSucceededEvent) aggregate.pay(mockGateway);

            assertThat(event).isNotNull();
            assertThat(event.getTransactionId()).isNotNull();
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(aggregate.transactionId()).isNotNull();
        }

        @Test
        @DisplayName("非待支付状态支付抛出异常")
        void pay_withNonPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.pay(mockGateway))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("refund 退款方法")
    class RefundTests {

        @Test
        @DisplayName("已支付状态全额退款成功")
        void refund_withSuccessStatus_fullRefund() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            var event = aggregate.refund(new BigDecimal("100.00"), mockGateway);

            assertThat(event).isNotNull();
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(aggregate.refundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("已支付状态部分退款成功")
        void refund_withSuccessStatus_partialRefund() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            var event = aggregate.refund(new BigDecimal("30.00"), mockGateway);

            assertThat(event).isNotNull();
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(aggregate.refundedAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("退款金额超过支付金额抛出异常")
        void refund_withAmountExceed_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("150.00"), mockGateway))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("累计退款金额超过支付金额抛出异常")
        void refund_withTotalExceed_throws() {
            PaymentAggregate aggregate = createRefundedAggregate(new BigDecimal("80.00"));

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("30.00"), mockGateway))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("累计退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("待支付状态不能退款")
        void refund_withPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("100.00"), mockGateway))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("fail 失败方法")
    class FailTests {

        @Test
        @DisplayName("待支付状态标记失败成功")
        void fail_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            PaymentFailedEvent event = aggregate.fail("支付超时");

            assertThat(event).isNotNull();
            assertThat(event.getReason()).isEqualTo("支付超时");
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("非待支付状态标记失败抛出异常")
        void fail_withNonPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.fail("支付超时"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有待支付状态可以标记为失败");
        }
    }

    @Nested
    @DisplayName("close 关闭方法")
    class CloseTests {

        @Test
        @DisplayName("待支付状态关闭成功")
        void close_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            PaymentClosedEvent event = aggregate.close();

            assertThat(aggregate.status()).isEqualTo(PaymentStatus.CLOSED);
            assertThat(event).isNotNull();
            assertThat(event.getPaymentId()).isEqualTo(aggregate.id());
        }

        @Test
        @DisplayName("支付失败状态关闭成功")
        void close_withFailedStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.FAILED);

            PaymentClosedEvent event = aggregate.close();

            assertThat(aggregate.status()).isEqualTo(PaymentStatus.CLOSED);
            assertThat(event).isNotNull();
        }

        @Test
        @DisplayName("已支付状态关闭抛出异常")
        void close_withSuccessStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.close())
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("cancelPay 取消支付方法")
    class CancelPayTests {

        @Test
        @DisplayName("PAYING 状态取消支付回退到 PENDING")
        void cancelPay_withPayingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PAYING);

            aggregate.cancelPay();

            assertThat(aggregate.status()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("非 PAYING 状态取消支付抛出异常")
        void cancelPay_withNonPayingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.cancelPay())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有支付中状态可以取消支付");
        }
    }

    @Nested
    @DisplayName("cancelRefund 取消退款方法")
    class CancelRefundTests {

        @Test
        @DisplayName("REFUNDING 状态取消退款回退到 SUCCESS")
        void cancelRefund_withRefundingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.REFUNDING);

            aggregate.cancelRefund();

            assertThat(aggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("非 REFUNDING 状态取消退款抛出异常")
        void cancelRefund_withNonRefundingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.cancelRefund())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有退款中状态可以取消退款");
        }
    }

    @Nested
    @DisplayName("非法状态转换测试")
    class InvalidTransitionTests {

        @Test
        @DisplayName("已关闭状态不能支付")
        void pay_withClosedStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.CLOSED);

            assertThatThrownBy(() -> aggregate.pay(mockGateway))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("已退款状态不能再次退款")
        void refund_withRefundedStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.REFUNDED);

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("50.00"), mockGateway))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("已关闭状态不能退款")
        void refund_withClosedStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.CLOSED);

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("50.00"), mockGateway))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("已支付状态不能标记为失败")
        void fail_withSuccessStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.fail("test"))
                .isInstanceOf(BusinessException.class);
        }
    }

    private PaymentAggregate createTestAggregate(PaymentStatus status) {
        return PaymentAggregate.reconstruct(
            System.currentTimeMillis(), "PAY" + System.currentTimeMillis(),
            1001L, 2001L, new BigDecimal("100.00"), BigDecimal.ZERO, 1,
            status, null, null, null, null, null, null
        );
    }

    private PaymentAggregate createRefundedAggregate(BigDecimal refundedAmount) {
        return PaymentAggregate.reconstruct(
            System.currentTimeMillis(), "PAY" + System.currentTimeMillis(),
            1001L, 2001L, new BigDecimal("100.00"), refundedAmount, 1,
            PaymentStatus.PARTIALLY_REFUNDED, null, null, null, null, null, null
        );
    }
}
