package com.cartethyia.easyorange.common.domain;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money 值对象测试")
class MoneyTest {

    @Nested
    @DisplayName("构造与工厂")
    class ConstructionTests {

        @Test
        @DisplayName("of 创建金额并按两位小数舍入")
        void of_roundsToTwoDecimals() {
            Money money = Money.of(new BigDecimal("10.005"));

            assertThat(money.value()).isEqualByComparingTo(new BigDecimal("10.01"));
        }

        @Test
        @DisplayName("ZERO 常量")
        void zero_constant() {
            assertThat(Money.ZERO.value()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("空金额抛出异常")
        void nullAmount_throws() {
            assertThatThrownBy(() -> Money.of(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("负金额抛出异常")
        void negativeAmount_throws() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-1")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("比较方法")
    class ComparisonTests {

        @Test
        @DisplayName("compareTo 按数值比较")
        void compareTo_byValue() {
            assertThat(Money.of(new BigDecimal("10")).compareTo(Money.of(new BigDecimal("20")))).isNegative();
            assertThat(Money.of(new BigDecimal("20")).compareTo(Money.of(new BigDecimal("10")))).isPositive();
        }

        @Test
        @DisplayName("isGreaterThan")
        void isGreaterThan() {
            assertThat(Money.of(new BigDecimal("20")).isGreaterThan(Money.of(new BigDecimal("10")))).isTrue();
            assertThat(Money.of(new BigDecimal("10")).isGreaterThan(Money.of(new BigDecimal("20")))).isFalse();
        }

        @Test
        @DisplayName("isLessThanOrEqualTo")
        void isLessThanOrEqualTo() {
            assertThat(Money.of(new BigDecimal("10")).isLessThanOrEqualTo(Money.of(new BigDecimal("10")))).isTrue();
            assertThat(Money.of(new BigDecimal("20")).isLessThanOrEqualTo(Money.of(new BigDecimal("10")))).isFalse();
        }

        @Test
        @DisplayName("isEqualTo")
        void isEqualTo() {
            assertThat(Money.of(new BigDecimal("10")).isEqualTo(Money.of(new BigDecimal("10.00")))).isTrue();
            assertThat(Money.of(new BigDecimal("10")).isEqualTo(Money.of(new BigDecimal("11")))).isFalse();
        }
    }

    @Nested
    @DisplayName("运算")
    class ArithmeticTests {

        @Test
        @DisplayName("add 相加")
        void add_sums() {
            Money result = Money.of(new BigDecimal("10")).add(Money.of(new BigDecimal("5")));

            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("multiply 乘法")
        void multiply_scales() {
            Money result = Money.of(new BigDecimal("10")).multiply(3);

            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("multiply 负乘数抛出异常")
        void multiply_negative_throws() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("10")).multiply(-1))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("toString 输出纯数字")
    void toString_plainString() {
        assertThat(Money.of(new BigDecimal("10.5")).toString()).isEqualTo("10.50");
    }
}