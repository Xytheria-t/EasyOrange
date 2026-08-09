package com.cartethyia.easyorange.payment.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("PaymentAction 状态守卫测试")
class PaymentActionTest {

    @Nested
    @DisplayName("PAY")
    class PayTests {

        @Test
        @DisplayName("PENDING 状态可以支付")
        void pay_pending() {
            assertThat(PaymentAction.PAY.canApply(PaymentStatus.PENDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PENDING"})
        @DisplayName("非 PENDING 状态不能支付")
        void pay_nonPending(PaymentStatus status) {
            assertThat(PaymentAction.PAY.canApply(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("REFUND")
    class RefundTests {

        @Test
        @DisplayName("SUCCESS 状态可以退款")
        void refund_success() {
            assertThat(PaymentAction.REFUND.canApply(PaymentStatus.SUCCESS)).isTrue();
        }

        @Test
        @DisplayName("PARTIALLY_REFUNDED 状态可以退款")
        void refund_partiallyRefunded() {
            assertThat(PaymentAction.REFUND.canApply(PaymentStatus.PARTIALLY_REFUNDED))
                    .isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"SUCCESS", "PARTIALLY_REFUNDED"})
        @DisplayName("其他状态不能退款")
        void refund_otherStatus(PaymentStatus status) {
            assertThat(PaymentAction.REFUND.canApply(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("CLOSE")
    class CloseTests {

        @Test
        @DisplayName("PENDING 状态可以关闭")
        void close_pending() {
            assertThat(PaymentAction.CLOSE.canApply(PaymentStatus.PENDING)).isTrue();
        }

        @Test
        @DisplayName("FAILED 状态可以关闭")
        void close_failed() {
            assertThat(PaymentAction.CLOSE.canApply(PaymentStatus.FAILED)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PENDING", "FAILED"})
        @DisplayName("其他状态不能关闭")
        void close_otherStatus(PaymentStatus status) {
            assertThat(PaymentAction.CLOSE.canApply(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("FAIL")
    class FailTests {

        @Test
        @DisplayName("PENDING 状态可以标记失败")
        void fail_pending() {
            assertThat(PaymentAction.FAIL.canApply(PaymentStatus.PENDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PENDING"})
        @DisplayName("非 PENDING 状态不能标记失败")
        void fail_nonPending(PaymentStatus status) {
            assertThat(PaymentAction.FAIL.canApply(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("CONFIRM_PAY")
    class ConfirmPayTests {

        @Test
        @DisplayName("PAYING 状态可以确认支付")
        void confirmPay_paying() {
            assertThat(PaymentAction.CONFIRM_PAY.canApply(PaymentStatus.PAYING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PAYING"})
        @DisplayName("非 PAYING 状态不能确认支付")
        void confirmPay_nonPaying(PaymentStatus status) {
            assertThat(PaymentAction.CONFIRM_PAY.canApply(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("CONFIRM_REFUND")
    class ConfirmRefundTests {

        @Test
        @DisplayName("REFUNDING 状态可以确认退款")
        void confirmRefund_refunding() {
            assertThat(PaymentAction.CONFIRM_REFUND.canApply(PaymentStatus.REFUNDING))
                    .isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"REFUNDING"})
        @DisplayName("非 REFUNDING 状态不能确认退款")
        void confirmRefund_nonRefunding(PaymentStatus status) {
            assertThat(PaymentAction.CONFIRM_REFUND.canApply(status)).isFalse();
        }
    }
}
