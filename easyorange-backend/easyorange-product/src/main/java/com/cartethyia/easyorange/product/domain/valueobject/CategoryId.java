package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record CategoryId(Long value) {
    public CategoryId {
        BizRequire.notNull(value, "分类ID不能为空");
        BizRequire.positive(value, "分类ID必须为正数");
    }
}