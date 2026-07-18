package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SellerId(@JsonValue String value) {

    public SellerId {
        BizRequire.notBlank(value, "资产方ID不能为空");
    }

    @JsonCreator
    public static SellerId of(String value) {
        return new SellerId(value);
    }
}
