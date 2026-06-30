package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record UserId(String value) {
    public UserId {
        BizRequire.notBlank(value, "用户ID不能为空");
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
