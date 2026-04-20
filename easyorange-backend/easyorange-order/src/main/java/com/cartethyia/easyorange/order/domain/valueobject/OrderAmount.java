package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;
import java.util.Objects;

public final class OrderAmount implements ValueObject {

    private final BigDecimal value;

    public OrderAmount(BigDecimal value) {
        BizRequire.notNull(value, "订单金额不能为空");
        BizRequire.isTrue(value.compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");
        this.value = value;
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderAmount that = (OrderAmount) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderAmount{" + value + '}';
    }
}
