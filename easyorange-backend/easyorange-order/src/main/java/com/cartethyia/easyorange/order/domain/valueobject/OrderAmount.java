package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.math.BigDecimal;

public record OrderAmount(BigDecimal value) {
    public OrderAmount {
        BizRequire.notNull(value, "订单金额不能为空");
        BizRequire.isTrue(value.compareTo(BigDecimal.ZERO) > 0, "订单金额必须大于0");
    }
}