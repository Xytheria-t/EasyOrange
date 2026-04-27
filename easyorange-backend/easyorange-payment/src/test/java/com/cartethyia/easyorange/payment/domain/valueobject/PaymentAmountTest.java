package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentAmount 值对象测试")
class PaymentAmountTest {

    @Nested
    @DisplayName("of 静态工厂方法 - 正常场景")
    class OfValidTests {

        @Test
        @DisplayName("使用 of 创建正金额")
        void of_withPositiveAmount_createsPaymentAmount() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount.value()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("使用 of 创建小金额")
        void of_withSmallAmount_createsPaymentAmount() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("0.01"));
            assertThat(amount.value()).isEqualByComparingTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("使用 of 创建大金额")
        void of_withLargeAmount_createsPaymentAmount() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("999999.99"));
            assertThat(amount.value()).isEqualByComparingTo(new BigDecimal("999999.99"));
        }
    }

    @Nested
    @DisplayName("of 静态工厂方法 - 非法输入")
    class OfInvalidTests {

        @Test
        @DisplayName("null 抛出 BusinessException")
        void of_withNull_throws() {
            assertThatThrownBy(() -> PaymentAmount.of(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额不能为空");
        }

        @Test
        @DisplayName("零金额抛出 BusinessException")
        void of_withZero_throws() {
            assertThatThrownBy(() -> PaymentAmount.of(BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额必须大于0");
        }

        @Test
        @DisplayName("负金额抛出 BusinessException")
        void of_withNegative_throws() {
            assertThatThrownBy(() -> PaymentAmount.of(new BigDecimal("-100.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额必须大于0");
        }
    }

    @Nested
    @DisplayName("比较方法")
    class ComparisonTests {

        @Test
        @DisplayName("isGreaterThan 返回 true 当金额大于比较值")
        void isGreaterThan_greaterValue_returnsTrue() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount.isGreaterThan(new BigDecimal("50.00"))).isTrue();
        }

        @Test
        @DisplayName("isGreaterThan 返回 false 当金额等于比较值")
        void isGreaterThan_equalValue_returnsFalse() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount.isGreaterThan(new BigDecimal("100.00"))).isFalse();
        }

        @Test
        @DisplayName("isLessThanOrEqualTo 返回 true 当金额小于等于比较值")
        void isLessThanOrEqualTo_smallerValue_returnsTrue() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("50.00"));
            assertThat(amount.isLessThanOrEqualTo(new BigDecimal("100.00"))).isTrue();
        }

        @Test
        @DisplayName("isLessThanOrEqualTo 返回 true 当金额等于比较值")
        void isLessThanOrEqualTo_equalValue_returnsTrue() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount.isLessThanOrEqualTo(new BigDecimal("100.00"))).isTrue();
        }

        @Test
        @DisplayName("isEqualTo 返回 true 当金额等于比较值")
        void isEqualTo_sameValue_returnsTrue() {
            PaymentAmount amount1 = PaymentAmount.of(new BigDecimal("100.00"));
            PaymentAmount amount2 = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount1.isEqualTo(amount2.value())).isTrue();
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同金额相等")
        void equals_sameValue_returnsTrue() {
            PaymentAmount amount1 = PaymentAmount.of(new BigDecimal("100.00"));
            PaymentAmount amount2 = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount1).isEqualTo(amount2);
        }

        @Test
        @DisplayName("不同金额不相等")
        void equals_differentValue_returnsFalse() {
            PaymentAmount amount1 = PaymentAmount.of(new BigDecimal("100.00"));
            PaymentAmount amount2 = PaymentAmount.of(new BigDecimal("200.00"));
            assertThat(amount1).isNotEqualTo(amount2);
        }

        @Test
        @DisplayName("相同金额 hashCode 相等")
        void hashCode_sameValue_returnsSameHash() {
            PaymentAmount amount1 = PaymentAmount.of(new BigDecimal("100.00"));
            PaymentAmount amount2 = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount1.hashCode()).isEqualTo(amount2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回金额字符串")
        void toString_returnsValueAsString() {
            PaymentAmount amount = PaymentAmount.of(new BigDecimal("100.00"));
            assertThat(amount.toString()).isEqualTo("100.00");
        }
    }
}
