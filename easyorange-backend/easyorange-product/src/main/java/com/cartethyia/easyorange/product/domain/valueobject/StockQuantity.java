package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class StockQuantity implements ValueObject {

    private final Integer value;

    public StockQuantity(Integer value) {
        BizRequire.notNull(value, "库存数量不能为空");
        BizRequire.isTrue(value >= 0, "库存数量不能为负数");
        this.value = value;
    }

    public Integer value() {
        return value;
    }

    public boolean isAvailable() {
        return value > 0;
    }

    public StockQuantity decrease() {
        return new StockQuantity(value - 1);
    }

    public StockQuantity increase() {
        return new StockQuantity(value + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockQuantity that = (StockQuantity) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "StockQuantity{" + value + '}';
    }
}
