package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record StockQuantity(Integer value) {

    public StockQuantity {
        BizRequire.notNull(value, "库存数量不能为空");
        BizRequire.nonNegative(value, "库存数量不能为负数");
    }

    public static StockQuantity of(Integer value) {
        return new StockQuantity(value);
    }

    public boolean isAvailable() {
        return value > 0;
    }

    public StockQuantity decrease(int amount) {
        int newValue = value - amount;
        BizRequire.nonNegative(newValue, "库存扣减后不能为负数, 当前: " + value + ", 扣减: " + amount);
        return new StockQuantity(newValue);
    }

    public StockQuantity decrease() {
        return decrease(1);
    }

    public StockQuantity increase(int amount) {
        return new StockQuantity(value + amount);
    }

    public StockQuantity increase() {
        return increase(1);
    }
}
