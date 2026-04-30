package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;

public record Money(BigDecimal value) {
    public Money {
        BizRequire.notNull(value, "金额不能为空");
        BizRequire.requireTrue(value.compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(Long cents) {
        return new Money(BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100)));
    }

    public boolean isGreaterThan(BigDecimal other) {
        return value.compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(BigDecimal other) {
        return value.compareTo(other) >= 0;
    }

    public boolean isLessThan(BigDecimal other) {
        return value.compareTo(other) < 0;
    }
}