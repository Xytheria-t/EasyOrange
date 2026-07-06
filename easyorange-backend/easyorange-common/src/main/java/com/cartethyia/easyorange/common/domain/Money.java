package com.cartethyia.easyorange.common.domain;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(@JsonValue BigDecimal value) implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        BizRequire.notNull(value, "金额不能为空");
        BizRequire.requireTrue(value.compareTo(BigDecimal.ZERO) >= 0, "金额不能为负数");
        value = value.setScale(SCALE, ROUNDING_MODE);
    }

    // ---- 工厂方法 ----

    @JsonCreator
    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(long cents) {
        return new Money(BigDecimal.valueOf(cents, SCALE));
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    @Override
    public int compareTo(@Nonnull Money other) {
        return value.compareTo(other.value);
    }

    // ---- 查询方法 ----

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return compareTo(other) >= 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        return compareTo(other) <= 0;
    }

    public boolean isEqualTo(Money other) {
        return compareTo(other) == 0;
    }

    // ---- 算术方法 ----

    public Money add(Money other) {
        return new Money(value.add(other.value));
    }

    public Money subtract(Money other) {
        BizRequire.requireTrue(value.compareTo(other.value) >= 0, "减后金额不能为负数");
        return new Money(value.subtract(other.value));
    }

    public Money multiply(int multiplier) {
        BizRequire.requireTrue(multiplier >= 0, "乘数不能为负数");
        return new Money(value.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money multiply(BigDecimal multiplier) {
        BizRequire.notNull(multiplier, "乘数不能为空");
        BizRequire.requireTrue(multiplier.compareTo(BigDecimal.ZERO) >= 0, "乘数不能为负数");
        return new Money(value.multiply(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        BizRequire.notNull(divisor, "除数不能为空");
        BizRequire.requireTrue(divisor.compareTo(BigDecimal.ZERO) > 0, "除数必须为正数");
        return new Money(value.divide(divisor, SCALE, ROUNDING_MODE));
    }

    public Money min(Money other) {
        return compareTo(other) <= 0 ? this : other;
    }

    public Money max(Money other) {
        return compareTo(other) >= 0 ? this : other;
    }

    @Nonnull
    @Override
    public String toString() {
        return value.toPlainString();
    }
}
