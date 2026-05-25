package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;

public record Money(BigDecimal amount) {
    public Money {
        BizRequire.notNull(amount, "金额不能为空");
        BizRequire.requireTrue(amount.compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money multiply(int multiplier) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }
}
