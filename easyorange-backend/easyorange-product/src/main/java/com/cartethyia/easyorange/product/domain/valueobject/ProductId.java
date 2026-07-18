package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ProductId(@JsonValue String value) {

    public ProductId {
        BizRequire.notBlank(value, "资产ID不能为空");
    }

    @JsonCreator
    public static ProductId of(String value) {
        return new ProductId(value);
    }
}
