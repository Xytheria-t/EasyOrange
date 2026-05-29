package com.cartethyia.easyorange.common.domain;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal value) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY));

    public Money {
        BizRequire.notNull(value, "金额不能为空");
        BizRequire.requireTrue(value.compareTo(BigDecimal.ZERO) >= 0, "金额不能为负数");
        value = value.setScale(SCALE, ROUNDING_MODE);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(long cents) {
        return new Money(BigDecimal.valueOf(cents, SCALE));
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return value.compareTo(other.value) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return value.compareTo(other.value) >= 0;
    }

    public boolean isLessThan(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return value.compareTo(other.value) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return value.compareTo(other.value) <= 0;
    }

    public boolean isEqualTo(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return value.compareTo(other.value) == 0;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "加数不能为空");
        return new Money(value.add(other.value));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "减数不能为空");
        return new Money(value.subtract(other.value));
    }

    public Money multiply(int multiplier) {
        BizRequire.requireTrue(multiplier >= 0, "乘数不能为负数");
        return new Money(value.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "乘数不能为空");
        BizRequire.requireTrue(multiplier.compareTo(BigDecimal.ZERO) >= 0, "乘数不能为负数");
        return new Money(value.multiply(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "除数不能为空");
        BizRequire.requireTrue(divisor.compareTo(BigDecimal.ZERO) > 0, "除数必须大于0");
        return new Money(value.divide(divisor, SCALE, ROUNDING_MODE));
    }

    public Money divide(int divisor) {
        BizRequire.requireTrue(divisor > 0, "除数必须大于0");
        return new Money(value.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING_MODE));
    }

    public Money min(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return isLessThanOrEqual(other) ? this : other;
    }

    public Money max(Money other) {
        Objects.requireNonNull(other, "比较金额不能为空");
        return isGreaterThanOrEqual(other) ? this : other;
    }

    public Money negate() {
        return new Money(value.negate());
    }

    public Money abs() {
        return new Money(value.abs());
    }

    public long toCents() {
        return value.movePointRight(SCALE).longValue();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
