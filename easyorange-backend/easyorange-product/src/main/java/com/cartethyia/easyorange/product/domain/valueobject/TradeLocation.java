package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record TradeLocation(String value) {

    public static final int MAX_LENGTH = 200;

    public TradeLocation {
        if (value != null) {
            value = value.trim();
            BizRequire.requireTrue(value.length() <= MAX_LENGTH, "交易地点长度不能超过 " + MAX_LENGTH + " 个字符");
        }
    }

    public static TradeLocation of(String value) {
        return new TradeLocation(value);
    }
}
