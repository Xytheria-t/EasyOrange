package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

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
