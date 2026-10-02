package com.cartethyia.easyorange.common.domain;

import com.cartethyia.easyorange.common.util.BizRequire;

/**
 * 乐观锁版本号 — 与 {@code @Version} 列对应（eo_order.version / eo_product.version）。
 * <p>
 * 重建聚合根时带回、写回时参与 {@code WHERE version = ?} 条件，冲突由仓储抛
 * {@code ConcurrentUpdateException}。order 与 product 两个聚合根的版本语义完全同构，
 * 故共用本值对象而非各写一份。
 */
public record Version(Integer value) {

    public static final Version INITIAL = new Version(0);

    public Version {
        BizRequire.notNull(value, "版本号不能为空");
        BizRequire.requireTrue(value >= 0, "版本号不能为负数");
    }

    public static Version of(Integer value) {
        return new Version(value);
    }
}
