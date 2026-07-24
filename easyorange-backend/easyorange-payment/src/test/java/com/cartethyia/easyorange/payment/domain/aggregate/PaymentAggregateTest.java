package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentAggregate 聚合根测试")
class PaymentAggregateTest {

    @Nested
    @DisplayName("create 静态工厂方法")
    class CreateTests {

        @Test
        @DisplayName("创建支付成功，返回不可变聚合根和事件")
        void create_withValidParams_createsPayment() {
            PaymentAggregate.PaymentCreatedResult result = PaymentAggregate.create(
                "1001", "1001", "2001", new BigDecimal("99.99"), PaymentMethod.WECHAT, "attach_data"
            );

            assertThat(result.aggregate().id()).isNotNull();
            assertThat(result.aggregate().paymentNo()).startsWith("PAY");
            assertThat(result.aggregate().orderId()).isEqualTo("1001");
            assertThat(result.aggregate().userId()).isEqualTo("2001");
            assertThat(result.aggregate().amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(result.aggregate().paymentMethod()).isEqualTo(PaymentMethod.WECHAT);
            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.event()).isInstanceOf(PaymentCreatedEvent.class);
        }

        @Test
        @DisplayName("create 返回的事件包含正确信息")
        void create_containsCreatedEvent() {
            PaymentAggregate.PaymentCreatedResult result = PaymentAggregate.create(
                "1001", "1001", "2001", new BigDecimal("99.99"), PaymentMethod.WECHAT, "attach_data"
            );

            PaymentCreatedEvent event = result.event();
            assertThat(event.paymentId()).isEqualTo(result.aggregate().id());
            assertThat(event.paymentNo()).isEqualTo(result.aggregate().paymentNo());
            assertThat(event.orderId()).isEqualTo("1001");
            assertThat(event.userId()).isEqualTo("2001");
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(event.paymentMethod()).isEqualTo("1");
        }

        @Test
        @DisplayName("orderId 为空抛出异常")
        void create_withNullOrderId_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                "1001", null, "2001", new BigDecimal("99.99"), PaymentMethod.WECHAT, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("订单ID不能为空");
        }

        @Test
        @DisplayName("userId 为空抛出异常")
        void create_withNullUserId_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                "1001", "1001", null, new BigDecimal("99.99"), PaymentMethod.WECHAT, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("用户ID不能为空");
        }

        @Test
        @DisplayName("amount 为空抛出异常")
        void create_withNullAmount_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                "1001", "1001", "2001", null, PaymentMethod.WECHAT, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付金额不能为空");
        }

        @Test
        @DisplayName("amount 小于等于零抛出异常")
        void create_withZeroAmount_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                "1001", "1001", "2001", BigDecimal.ZERO, PaymentMethod.WECHAT, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付金额必须大于0");
        }

        @Test
        @DisplayName("paymentMethod 为空抛出异常")
        void create_withNullPaymentMethod_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                "1001", "1001", "2001", new BigDecimal("99.99"), null, null
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
                "1001", "PAY123456", "2001", "3001",
                new BigDecimal("99.99"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                PaymentStatus.SUCCESS, "TXN123", "已退款",
                LocalDateTime.now(), "attach",
                LocalDateTime.now(), LocalDateTime.now(), 1
            );

            assertThat(aggregate.id()).isEqualTo("1001");
            assertThat(aggregate.paymentNo()).isEqualTo("PAY123456");
            assertThat(aggregate.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(aggregate.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("reconstruct 处理 version 为 null")
        void reconstruct_withNullVersion() {
            PaymentAggregate aggregate = PaymentAggregate.reconstruct(
                "1001", "PAY123456", "2001", "3001",
                new BigDecimal("99.99"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                PaymentStatus.PENDING, null, null, null, null, null, null, null
            );

            assertThat(aggregate.version()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("preparePay / confirmPay 两阶段支付")
    class PayTests {

        @Test
        @DisplayName("待支付状态 preparePay 成功")
        void preparePay_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            PaymentAggregate.PayPreparedResult result = aggregate.preparePay();

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.PAYING);
            assertThat(result.aggregate().version()).isEqualTo(1);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
        }

        @Test
        @DisplayName("PAYING 状态 confirmPay 成功")
        void confirmPay_withPayingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PAYING);

            PaymentAggregate.PayConfirmedResult result = aggregate.confirmPay(PaymentResult.success("TXN_001"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(result.aggregate().transactionId()).isEqualTo("TXN_001");
            assertThat(result.event()).isInstanceOf(PaymentSucceededEvent.class);
            PaymentSucceededEvent event = (PaymentSucceededEvent) result.event();
            assertThat(event.transactionId()).isEqualTo("TXN_001");
        }

        @Test
        @DisplayName("PAYING 状态 confirmPay 失败")
        void confirmPay_withPayingStatus_failure() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PAYING);

            PaymentAggregate.PayConfirmedResult result = aggregate.confirmPay(PaymentResult.failure("余额不足"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.event()).isInstanceOf(PaymentFailedEvent.class);
            PaymentFailedEvent event = (PaymentFailedEvent) result.event();
            assertThat(event.reason()).isEqualTo("余额不足");
        }

        @Test
        @DisplayName("非待支付状态 preparePay 抛出异常")
        void preparePay_withNonPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(aggregate::preparePay)
                .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("非 PAYING 状态 confirmPay 抛出异常")
        void confirmPay_withNonPayingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.confirmPay(PaymentResult.success("TXN")))
                .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("prepareRefund / confirmRefund 两阶段退款")
    class RefundTests {

        @Test
        @DisplayName("已支付状态 prepareRefund 成功")
        void prepareRefund_withSuccessStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            PaymentAggregate.RefundPreparedResult result = aggregate.prepareRefund(new BigDecimal("100.00"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.REFUNDING);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
        }

        @Test
        @DisplayName("REFUNDING 状态 confirmRefund 全额退款成功")
        void confirmRefund_withRefundingStatus_fullRefund() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.REFUNDING);

            PaymentAggregate.RefundConfirmedResult result = aggregate.confirmRefund(RefundResult.success("REF_001"), new BigDecimal("100.00"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(result.aggregate().refundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.event()).isInstanceOf(PaymentRefundedEvent.class);
        }

        @Test
        @DisplayName("REFUNDING 状态 confirmRefund 部分退款成功")
        void confirmRefund_withRefundingStatus_partialRefund() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.REFUNDING);

            PaymentAggregate.RefundConfirmedResult result = aggregate.confirmRefund(RefundResult.success("REF_001"), new BigDecimal("30.00"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(result.aggregate().refundedAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("prepareRefund 退款金额超过支付金额抛出异常")
        void prepareRefund_withAmountExceed_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("150.00")))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("prepareRefund 累计退款金额超过支付金额抛出异常")
        void prepareRefund_withTotalExceed_throws() {
            PaymentAggregate aggregate = createRefundedAggregate(new BigDecimal("80.00"));

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("30.00")))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("累计退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("待支付状态不能准备退款")
        void prepareRefund_withPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("100.00")))
                .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("confirmRefund 失败时回退到 SUCCESS")
        void confirmRefund_withGatewayFailure_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.REFUNDING);

            assertThatThrownBy(() -> aggregate.confirmRefund(RefundResult.failure("网关超时"), new BigDecimal("100.00")))
                .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("fail 失败方法")
    class FailTests {

        @Test
        @DisplayName("待支付状态标记失败成功")
        void fail_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            PaymentAggregate.FailedResult result = aggregate.fail("支付超时");

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
            assertThat(result.event()).isInstanceOf(PaymentFailedEvent.class);
            PaymentFailedEvent event = result.event();
            assertThat(event.reason()).isEqualTo("支付超时");
        }

        @Test
        @DisplayName("非待支付状态标记失败抛出异常")
        void fail_withNonPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.fail("支付超时"))
                .isInstanceOf(PaymentDomainException.class)
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

            PaymentAggregate.ClosedResult result = aggregate.close();

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.CLOSED);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
            assertThat(result.event()).isInstanceOf(PaymentClosedEvent.class);
            PaymentClosedEvent event = result.event();
            assertThat(event.paymentId()).isEqualTo(aggregate.id());
        }

        @Test
        @DisplayName("支付失败状态关闭成功")
        void close_withFailedStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.FAILED);

            PaymentAggregate.ClosedResult result = aggregate.close();

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.CLOSED);
            assertThat(result.event()).isInstanceOf(PaymentClosedEvent.class);
        }

        @Test
        @DisplayName("已支付状态关闭抛出异常")
        void close_withSuccessStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(aggregate::close)
                .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("cancelPay 取消支付方法")
    class CancelPayTests {

        @Test
        @DisplayName("PAYING 状态取消支付回退到 PENDING")
        void cancelPay_withPayingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PAYING);

            PaymentAggregate.CancelPayResult result = aggregate.cancelPay();

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
        }

        @Test
        @DisplayName("非 PAYING 状态取消支付抛出异常")
        void cancelPay_withNonPayingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(aggregate::cancelPay)
                .isInstanceOf(PaymentDomainException.class)
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

            PaymentAggregate.CancelRefundResult result = aggregate.cancelRefund();

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
        }

        @Test
        @DisplayName("非 REFUNDING 状态取消退款抛出异常")
        void cancelRefund_withNonRefundingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(aggregate::cancelRefund)
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("只有退款中状态可以取消退款");
        }
    }

    @Nested
    @DisplayName("directRefund 直接退款")
    class DirectRefundTests {

        @Test
        @DisplayName("SUCCESS 状态直接退款成功")
        void directRefund_withSuccessStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            PaymentAggregate.DirectRefundResult result = aggregate.directRefund("商品问题");

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(result.aggregate().refundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.event()).isInstanceOf(PaymentRefundedEvent.class);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
        }
    }

    @Nested
    @DisplayName("非法状态转换测试")
    class InvalidTransitionTests {

        @Test
        @DisplayName("已关闭状态不能 preparePay")
        void preparePay_withClosedStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.CLOSED);

            assertThatThrownBy(aggregate::preparePay)
                .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("已退款状态不能 prepareRefund")
        void prepareRefund_withRefundedStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.REFUNDED);

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("50.00")))
                .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("已关闭状态不能 prepareRefund")
        void prepareRefund_withClosedStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.CLOSED);

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("50.00")))
                .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("已支付状态不能标记为失败")
        void fail_withSuccessStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.fail("test"))
                .isInstanceOf(PaymentDomainException.class);
        }
    }

    private PaymentAggregate createTestAggregate(PaymentStatus status) {
        return PaymentAggregate.reconstruct(
            String.valueOf(System.currentTimeMillis()), "PAY" + System.currentTimeMillis(),
            "1001", "2001", new BigDecimal("100.00"), BigDecimal.ZERO, PaymentMethod.WECHAT,
            status, null, null, null, null, null, null, 0
        );
    }

    private PaymentAggregate createRefundedAggregate(BigDecimal refundedAmount) {
        return PaymentAggregate.reconstruct(
            String.valueOf(System.currentTimeMillis()), "PAY" + System.currentTimeMillis(),
            "1001", "2001", new BigDecimal("100.00"), refundedAmount, PaymentMethod.WECHAT,
            PaymentStatus.PARTIALLY_REFUNDED, null, null, null, null, null, null, 0
        );
    }
}
