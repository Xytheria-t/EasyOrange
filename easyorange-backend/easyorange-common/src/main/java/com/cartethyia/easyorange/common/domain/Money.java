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

    @JsonCreator
    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    @Override
    public int compareTo(@Nonnull Money other) {
        return value.compareTo(other.value);
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        return compareTo(other) <= 0;
    }

    public boolean isEqualTo(Money other) {
        return compareTo(other) == 0;
    }

    public Money add(Money other) {
        return new Money(value.add(other.value));
    }

    public Money multiply(int multiplier) {
        BizRequire.requireTrue(multiplier >= 0, "乘数不能为负数");
        return new Money(value.multiply(BigDecimal.valueOf(multiplier)));
    }

    @Nonnull
    @Override
    public String toString() {
        return value.toPlainString();
    }
}
