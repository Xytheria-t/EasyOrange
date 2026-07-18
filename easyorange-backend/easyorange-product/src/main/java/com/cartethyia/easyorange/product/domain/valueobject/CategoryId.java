package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record CategoryId(@JsonValue String value) {

    public CategoryId {
        BizRequire.notBlank(value, "分类ID不能为空");
    }

    @JsonCreator
    public static CategoryId of(String value) {
        return new CategoryId(value);
    }
}
