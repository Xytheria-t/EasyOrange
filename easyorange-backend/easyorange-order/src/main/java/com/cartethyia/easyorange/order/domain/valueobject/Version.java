package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

/**
 * 乐观锁版本号 — 与 {@code eo_order.version} 列（{@code @Version}）对应，
 * 重建聚合根时带回、写回时参与 {@code WHERE version=?} 条件，冲突由仓储抛 {@code ConcurrentUpdateException}。
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
