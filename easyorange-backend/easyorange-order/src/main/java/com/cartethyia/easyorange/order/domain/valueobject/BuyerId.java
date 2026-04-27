package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record BuyerId(Long value) {
    public BuyerId {
        BizRequire.notNull(value, "买家ID不能为空");
        BizRequire.positive(value, "买家ID必须为正数");
    }
}