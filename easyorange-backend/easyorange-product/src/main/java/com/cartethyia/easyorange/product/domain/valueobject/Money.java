package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;

public record Money(BigDecimal value) {
    public Money {
        BizRequire.notNull(value, "金额不能为空");
        BizRequire.isTrue(value.compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
    }

    public Money(Long cents) {
        this(BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100)));
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