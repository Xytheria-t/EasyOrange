package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ProductTitle(@JsonValue String value) {

    public static final int MAX_LENGTH = 200;

    public ProductTitle {
        BizRequire.notBlank(value, "资产名称不能为空");
        value = value.trim();
        BizRequire.requireTrue(value.length() <= MAX_LENGTH, "资产名称长度必须在 1-" + MAX_LENGTH + " 个字符之间");
    }

    @JsonCreator
    public static ProductTitle of(String value) {
        return new ProductTitle(value);
    }
}
