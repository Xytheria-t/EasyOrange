package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class CategoryId implements ValueObject {

    private final Long value;

    public CategoryId(Long value) {
        BizRequire.notNull(value, "分类ID不能为空");
        BizRequire.isTrue(value > 0, "分类ID必须为正数");
        this.value = value;
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryId categoryId = (CategoryId) o;
        return Objects.equals(value, categoryId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "CategoryId{" + value + '}';
    }
}
