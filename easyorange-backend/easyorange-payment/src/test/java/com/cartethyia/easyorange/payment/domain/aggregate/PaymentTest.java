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

@DisplayName("Payment 聚合根测试")
class PaymentTest {

    @Nested
    @DisplayName("create 静态工厂方法")
    class CreateTests {

        @Test
        @DisplayName("创建支付成功，返回不可变聚合根和事件")
        void create_withValidSpec_createsPayment() {
            var spec = new PaymentCreateSpec(
                    "1001", "1001", "2001", new BigDecimal("99.99"), PaymentMethod.WECHAT, "attach_data");
            var result = Payment.create(spec);

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
            var spec = new PaymentCreateSpec(
                    "1001", "1001", "2001", new BigDecimal("99.99"), PaymentMethod.WECHAT, "attach_data");
            var result = Payment.create(spec);

            PaymentCreatedEvent event = result.event();
            assertThat(event.paymentId()).isEqualTo(result.aggregate().id());
            assertThat(event.paymentNo()).isEqualTo(result.aggregate().paymentNo());
            assertThat(event.orderId()).isEqualTo("1001");
            assertThat(event.userId()).isEqualTo("2001");
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(event.paymentMethod()).isEqualTo("WECHAT");
        }

        @Test
        @DisplayName("orderId 为空抛出异常")
        void create_withNullOrderId_throws() {
            var spec = new PaymentCreateSpec(
                    "1001", null, "2001", new BigDecimal("99.99"), PaymentMethod.WECHAT, null);

            assertThatThrownBy(() -> Payment.create(spec))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单ID不能为空");
        }

        @Test
        @DisplayName("userId 为空抛出异常")
        void create_withNullUserId_throws() {
            var spec = new PaymentCreateSpec(
                    "1001", "1001", null, new BigDecimal("99.99"), PaymentMethod.WECHAT, null);

            assertThatThrownBy(() -> Payment.create(spec))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户ID不能为空");
        }

        @Test
        @DisplayName("amount 为空抛出异常")
        void create_withNullAmount_throws() {
            var spec = new PaymentCreateSpec(
                    "1001", "1001", "2001", null, PaymentMethod.WECHAT, null);

            assertThatThrownBy(() -> Payment.create(spec))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("支付金额不能为空");
        }

        @Test
        @DisplayName("amount 小于等于零抛出异常")
        void create_withZeroAmount_throws() {
            var spec = new PaymentCreateSpec(
                    "1001", "1001", "2001", BigDecimal.ZERO, PaymentMethod.WECHAT, null);

            assertThatThrownBy(() -> Payment.create(spec))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("支付金额必须大于0");
        }

        @Test
        @DisplayName("paymentMethod 为空抛出异常")
        void create_withNullPaymentMethod_throws() {
            var spec = new PaymentCreateSpec(
                    "1001", "1001", "2001", new BigDecimal("99.99"), null, null);

            assertThatThrownBy(() -> Payment.create(spec))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("支付方式不能为空");
        }
    }

    @Nested
    @DisplayName("from 重建方法")
    class ReconstructTests {

        @Test
        @DisplayName("from 正确重建聚合根")
        void from_convertsCorrectly() {
            var spec = new PaymentReconstructSpec(
                    "1001", "PAY123456", "2001", "3001",
                    new BigDecimal("99.99"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                    PaymentStatus.SUCCESS, "TXN123", "已退款",
                    LocalDateTime.now(), "attach",
                    LocalDateTime.now(), LocalDateTime.now(), 1
            );

            Payment aggregate = Payment.from(spec);

            assertThat(aggregate.id()).isEqualTo("1001");
            assertThat(aggregate.paymentNo()).isEqualTo("PAY123456");
            assertThat(aggregate.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(aggregate.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("from 处理 version 为 null")
        void from_withNullVersion() {
            var spec = new PaymentReconstructSpec(
                    "1001", "PAY123456", "2001", "3001",
                    new BigDecimal("99.99"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                    PaymentStatus.PENDING, null, null, null, null, null, null, null
            );

            Payment aggregate = Payment.from(spec);

            assertThat(aggregate.version()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("preparePay / confirmPay 两阶段支付")
    class PayTests {

        @Test
        @DisplayName("待支付状态 preparePay 成功返回中间态聚合根")
        void preparePay_withPendingStatus_success() {
            Payment aggregate = createTestAggregate(PaymentStatus.PENDING);

            Payment updated = aggregate.preparePay();

            assertThat(updated.status()).isEqualTo(PaymentStatus.PAYING);
            assertThat(updated.version()).isEqualTo(1);
            assertThat(updated).isNotSameAs(aggregate);
        }

        @Test
        @DisplayName("PAYING 状态 confirmPay 成功")
        void confirmPay_withPayingStatus_success() {
            Payment aggregate = createTestAggregate(PaymentStatus.PAYING);

            var result = aggregate.confirmPay(PaymentResult.success("TXN_001"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(result.aggregate().transactionId()).isEqualTo("TXN_001");
            assertThat(result.event())
                    .isInstanceOfSatisfying(PaymentSucceededEvent.class,
                            e -> assertThat(e.transactionId()).isEqualTo("TXN_001"));
        }

        @Test
        @DisplayName("PAYING 状态 confirmPay 失败")
        void confirmPay_withPayingStatus_failure() {
            Payment aggregate = createTestAggregate(PaymentStatus.PAYING);

            var result = aggregate.confirmPay(PaymentResult.failure("余额不足"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.event())
                    .isInstanceOfSatisfying(PaymentFailedEvent.class,
                            e -> assertThat(e.reason()).isEqualTo("余额不足"));
        }

        @Test
        @DisplayName("非待支付状态 preparePay 抛出异常")
        void preparePay_withNonPendingStatus_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(aggregate::preparePay)
                    .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("非 PAYING 状态 confirmPay 抛出异常")
        void confirmPay_withNonPayingStatus_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.confirmPay(PaymentResult.success("TXN")))
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("prepareRefund / confirmRefund 两阶段退款")
    class RefundTests {

        @Test
        @DisplayName("已支付状态 prepareRefund 成功返回中间态聚合根")
        void prepareRefund_withSuccessStatus_success() {
            Payment aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            Payment updated = aggregate.prepareRefund(new BigDecimal("100.00"));

            assertThat(updated.status()).isEqualTo(PaymentStatus.REFUNDING);
            assertThat(updated).isNotSameAs(aggregate);
        }

        @Test
        @DisplayName("REFUNDING 状态 confirmRefund 全额退款成功")
        void confirmRefund_withRefundingStatus_fullRefund() {
            Payment aggregate = createTestAggregate(PaymentStatus.REFUNDING);

            var result = aggregate.confirmRefund(RefundResult.success("REF_001"), new BigDecimal("100.00"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(result.aggregate().refundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.event()).isInstanceOf(PaymentRefundedEvent.class);
        }

        @Test
        @DisplayName("REFUNDING 状态 confirmRefund 部分退款成功")
        void confirmRefund_withRefundingStatus_partialRefund() {
            Payment aggregate = createTestAggregate(PaymentStatus.REFUNDING);

            var result = aggregate.confirmRefund(RefundResult.success("REF_001"), new BigDecimal("30.00"));

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(result.aggregate().refundedAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("prepareRefund 退款金额超过支付金额抛出异常")
        void prepareRefund_withAmountExceed_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("150.00")))
                    .isInstanceOf(PaymentDomainException.class)
                    .hasMessageContaining("退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("prepareRefund 累计退款金额超过支付金额抛出异常")
        void prepareRefund_withTotalExceed_throws() {
            Payment aggregate = createRefundedAggregate(new BigDecimal("80.00"));

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("30.00")))
                    .isInstanceOf(PaymentDomainException.class)
                    .hasMessageContaining("累计退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("待支付状态不能准备退款")
        void prepareRefund_withPendingStatus_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.prepareRefund(new BigDecimal("100.00")))
                    .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("confirmRefund 失败时抛出异常")
        void confirmRefund_withGatewayFailure_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.REFUNDING);

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
            Payment aggregate = createTestAggregate(PaymentStatus.PENDING);

            var result = aggregate.fail("支付超时");

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
            assertThat(result.event()).isInstanceOf(PaymentFailedEvent.class);
            PaymentFailedEvent event = result.event();
            assertThat(event.reason()).isEqualTo("支付超时");
        }

        @Test
        @DisplayName("非待支付状态标记失败抛出异常")
        void fail_withNonPendingStatus_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.SUCCESS);

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
            Payment aggregate = createTestAggregate(PaymentStatus.PENDING);

            var result = aggregate.close();

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.CLOSED);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
            assertThat(result.event()).isInstanceOf(PaymentClosedEvent.class);
        }

        @Test
        @DisplayName("非待支付状态关闭抛出异常")
        void close_withNonPendingStatus_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            assertThatThrownBy(aggregate::close)
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("directRefund 直接退款方法（dev mock 路径）")
    class DirectRefundTests {

        @Test
        @DisplayName("已支付状态直接退款成功")
        void directRefund_withSuccessStatus_success() {
            Payment aggregate = createTestAggregate(PaymentStatus.SUCCESS);

            var result = aggregate.directRefund("test refund");

            assertThat(result.aggregate().status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(result.aggregate()).isNotSameAs(aggregate);
            assertThat(result.event()).isInstanceOf(PaymentRefundedEvent.class);
        }

        @Test
        @DisplayName("非已支付状态直接退款抛出异常")
        void directRefund_withNonSuccessStatus_throws() {
            Payment aggregate = createTestAggregate(PaymentStatus.PENDING);

            assertThatThrownBy(() -> aggregate.directRefund("test refund"))
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    // ==================== Fixtures ====================

    private Payment createTestAggregate(PaymentStatus status) {
        var spec = new PaymentReconstructSpec(
                "1001", "PAY123456", "2001", "3001",
                new BigDecimal("100.00"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                status, "TXN123", null, null, "attach",
                LocalDateTime.now(), LocalDateTime.now(), 0
        );
        return Payment.from(spec);
    }

    private Payment createRefundedAggregate(BigDecimal refundedAmount) {
        var spec = new PaymentReconstructSpec(
                "1001", "PAY123456", "2001", "3001",
                new BigDecimal("100.00"), refundedAmount, PaymentMethod.WECHAT,
                PaymentStatus.SUCCESS, "TXN123", "previous refund",
                LocalDateTime.now(), "attach",
                LocalDateTime.now(), LocalDateTime.now(), 0
        );
        return Payment.from(spec);
    }
}
