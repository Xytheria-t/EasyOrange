package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money implements ValueObject {

    private final BigDecimal value;

    public Money(BigDecimal value) {
        BizRequire.notNull(value, "金额不能为空");
        BizRequire.isTrue(value.compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
        this.value = value;
    }

    public Money(Long cents) {
        BizRequire.notNull(cents, "金额不能为空");
        BizRequire.isTrue(cents > 0, "金额必须大于0");
        this.value = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100));
    }

    public BigDecimal value() {
        return value;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(value, money.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Money{" + value + '}';
    }
}
