package com.cartethyia.easyorange.payment.domain.specification;

import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentSpecification 规格测试")
class PaymentSpecificationTest {

    @Nested
    @DisplayName("canPay")
    class CanPayTests {

        @Test
        @DisplayName("PENDING 状态可以支付")
        void canPay_pending() {
            assertThat(PaymentSpecification.canPay(PaymentStatus.PENDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"PENDING"})
        @DisplayName("非 PENDING 状态不能支付")
        void canPay_nonPending(PaymentStatus status) {
            assertThat(PaymentSpecification.canPay(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canRefund")
    class CanRefundTests {

        @Test
        @DisplayName("SUCCESS 状态可以退款")
        void canRefund_success() {
            assertThat(PaymentSpecification.canRefund(PaymentStatus.SUCCESS)).isTrue();
        }

        @Test
        @DisplayName("PARTIALLY_REFUNDED 状态可以退款")
        void canRefund_partiallyRefunded() {
            assertThat(PaymentSpecification.canRefund(PaymentStatus.PARTIALLY_REFUNDED)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUCCESS", "PARTIALLY_REFUNDED"})
        @DisplayName("其他状态不能退款")
        void canRefund_otherStatus(PaymentStatus status) {
            assertThat(PaymentSpecification.canRefund(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canClose")
    class CanCloseTests {

        @Test
        @DisplayName("PENDING 状态可以关闭")
        void canClose_pending() {
            assertThat(PaymentSpecification.canClose(PaymentStatus.PENDING)).isTrue();
        }

        @Test
        @DisplayName("FAILED 状态可以关闭")
        void canClose_failed() {
            assertThat(PaymentSpecification.canClose(PaymentStatus.FAILED)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"PENDING", "FAILED"})
        @DisplayName("其他状态不能关闭")
        void canClose_otherStatus(PaymentStatus status) {
            assertThat(PaymentSpecification.canClose(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canFail")
    class CanFailTests {

        @Test
        @DisplayName("PENDING 状态可以标记失败")
        void canFail_pending() {
            assertThat(PaymentSpecification.canFail(PaymentStatus.PENDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"PENDING"})
        @DisplayName("非 PENDING 状态不能标记失败")
        void canFail_nonPending(PaymentStatus status) {
            assertThat(PaymentSpecification.canFail(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canConfirmPay")
    class CanConfirmPayTests {

        @Test
        @DisplayName("PAYING 状态可以确认支付")
        void canConfirmPay_paying() {
            assertThat(PaymentSpecification.canConfirmPay(PaymentStatus.PAYING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"PAYING"})
        @DisplayName("非 PAYING 状态不能确认支付")
        void canConfirmPay_nonPaying(PaymentStatus status) {
            assertThat(PaymentSpecification.canConfirmPay(status)).isFalse();
        }
    }

    @Nested
    @DisplayName("canConfirmRefund")
    class CanConfirmRefundTests {

        @Test
        @DisplayName("REFUNDING 状态可以确认退款")
        void canConfirmRefund_refunding() {
            assertThat(PaymentSpecification.canConfirmRefund(PaymentStatus.REFUNDING)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"REFUNDING"})
        @DisplayName("非 REFUNDING 状态不能确认退款")
        void canConfirmRefund_nonRefunding(PaymentStatus status) {
            assertThat(PaymentSpecification.canConfirmRefund(status)).isFalse();
        }
    }
}
