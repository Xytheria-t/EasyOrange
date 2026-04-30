package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record UserId(Long value) {
    public UserId {
        BizRequire.notNull(value, "用户ID不能为空");
        BizRequire.requireTrue(value > 0, "用户ID必须大于0");
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}
