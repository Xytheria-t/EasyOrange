package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record StockQuantity(@JsonValue Integer value) {

    public StockQuantity {
        BizRequire.notNull(value, "库存数量不能为空");
        BizRequire.requireTrue(value >= 0, "库存数量不能为负数");
    }

    @JsonCreator
    public static StockQuantity of(Integer value) {
        return new StockQuantity(value);
    }

    public boolean isAvailable() {
        return value > 0;
    }

    /**
     * 扣减库存；扣成负数即抛。
     * <p>
     * 正常路径的「够不够扣」由 {@code Product.decrementStock} 一次判定（并抛带 productId 的 B2003）。
     * 这道 {@link BizRequire} 是值对象层的最后防线：聚合根判定与实际写入之间的并发窗口
     * （乐观锁失败前的读改算）一旦漏到这里，宁可抛异常也不产出负库存。
     */
    public StockQuantity decrease(int amount) {
        int newValue = value - amount;
        BizRequire.requireTrue(newValue >= 0, "库存扣减后不能为负数, 当前: " + value + ", 扣减: " + amount);
        return new StockQuantity(newValue);
    }

    public StockQuantity increase(int amount) {
        return new StockQuantity(value + amount);
    }
}
