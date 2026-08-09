package com.cartethyia.easyorange.payment.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("PaymentStatusGuard 状态守卫测试")
class PaymentStatusGuardTest {

    @Nested
    @DisplayName("canPay")
    class CanPayTests {

        @Test
        @DisplayName("PENDING 状态可以支付")
        void canPay_pending() {
            assertThat(PaymentStatusGuard.canPay(PaymentStatus.PENDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PENDING"})
        @DisplayName("非 PENDING 状态不能支付")
        void canPay_nonPending(PaymentStatus status) {
            assertThat(PaymentStatusGuard.canPay(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canRefund")
    class CanRefundTests {

        @Test
        @DisplayName("SUCCESS 状态可以退款")
        void canRefund_success() {
            assertThat(PaymentStatusGuard.canRefund(PaymentStatus.SUCCESS)).isTrue();
        }

        @Test
        @DisplayName("PARTIALLY_REFUNDED 状态可以退款")
        void canRefund_partiallyRefunded() {
            assertThat(PaymentStatusGuard.canRefund(PaymentStatus.PARTIALLY_REFUNDED))
                    .isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"SUCCESS", "PARTIALLY_REFUNDED"})
        @DisplayName("其他状态不能退款")
        void canRefund_otherStatus(PaymentStatus status) {
            assertThat(PaymentStatusGuard.canRefund(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canClose")
    class CanCloseTests {

        @Test
        @DisplayName("PENDING 状态可以关闭")
        void canClose_pending() {
            assertThat(PaymentStatusGuard.canClose(PaymentStatus.PENDING)).isTrue();
        }

        @Test
        @DisplayName("FAILED 状态可以关闭")
        void canClose_failed() {
            assertThat(PaymentStatusGuard.canClose(PaymentStatus.FAILED)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PENDING", "FAILED"})
        @DisplayName("其他状态不能关闭")
        void canClose_otherStatus(PaymentStatus status) {
            assertThat(PaymentStatusGuard.canClose(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canFail")
    class CanFailTests {

        @Test
        @DisplayName("PENDING 状态可以标记失败")
        void canFail_pending() {
            assertThat(PaymentStatusGuard.canFail(PaymentStatus.PENDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PENDING"})
        @DisplayName("非 PENDING 状态不能标记失败")
        void canFail_nonPending(PaymentStatus status) {
            assertThat(PaymentStatusGuard.canFail(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canConfirmPay")
    class CanConfirmPayTests {

        @Test
        @DisplayName("PAYING 状态可以确认支付")
        void canConfirmPay_paying() {
            assertThat(PaymentStatusGuard.canConfirmPay(PaymentStatus.PAYING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"PAYING"})
        @DisplayName("非 PAYING 状态不能确认支付")
        void canConfirmPay_nonPaying(PaymentStatus status) {
            assertThat(PaymentStatusGuard.canConfirmPay(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canConfirmRefund")
    class CanConfirmRefundTests {

        @Test
        @DisplayName("REFUNDING 状态可以确认退款")
        void canConfirmRefund_refunding() {
            assertThat(PaymentStatusGuard.canConfirmRefund(PaymentStatus.REFUNDING))
                    .isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = PaymentStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"REFUNDING"})
        @DisplayName("非 REFUNDING 状态不能确认退款")
        void canConfirmRefund_nonRefunding(PaymentStatus status) {
            assertThat(PaymentStatusGuard.canConfirmRefund(status)).isFalse();
        }
    }
}
