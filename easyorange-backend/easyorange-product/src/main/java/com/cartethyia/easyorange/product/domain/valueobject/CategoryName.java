package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 分类名称 — 非空、长度受限；长度上限与 DTO 层的 {@code @Size(max = 20)} 对齐。 */
public record CategoryName(@JsonValue String value) {

    private static final int MAX_LENGTH = 20;

    public CategoryName {
        BizRequire.notBlank(value, "分类名称不能为空");
        BizRequire.requireTrue(value.length() <= MAX_LENGTH, "分类名称最长" + MAX_LENGTH + "个字符");
    }

    @JsonCreator
    public static CategoryName of(String value) {
        return new CategoryName(value);
    }
}
