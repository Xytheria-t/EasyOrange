package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record StockQuantity(Integer value) {
    public StockQuantity {
        BizRequire.notNull(value, "库存数量不能为空");
        BizRequire.nonNegative(value, "库存数量不能为负数");
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
}