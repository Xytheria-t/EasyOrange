package com.cartethyia.easyorange.common.domain;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(@JsonValue BigDecimal value) implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        BizRequire.notNull(value, "金额不能为空");
        BizRequire.requireTrue(value.compareTo(BigDecimal.ZERO) >= 0, "金额不能为负数");
        value = value.setScale(SCALE, ROUNDING_MODE);
    }

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
        Objects.requireNonNull(other, "比较金额不能为空");
        return value.compareTo(other.value);
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return compareTo(other) <= 0;
    }

    public boolean isEqualTo(Money other) {
        return compareTo(other) == 0;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "加数不能为空");
        return new Money(value.add(other.value));
    }

    public Money multiply(int multiplier) {
        BizRequire.requireTrue(multiplier >= 0, "乘数不能为负数");
        return new Money(value.multiply(BigDecimal.valueOf(multiplier)));
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
