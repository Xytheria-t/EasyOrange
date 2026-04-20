package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class ProductTitle implements ValueObject {

    private final String value;

    public ProductTitle(String value) {
        BizRequire.notBlank(value, "商品名称不能为空");
        BizRequire.isTrue(value.length() <= 200, "商品名称不能超过200个字符");
        this.value = value.trim();
    }

    public String value() {
        return value;
    }

    public String trimmed() {
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductTitle that = (ProductTitle) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ProductTitle{" + value + '}';
    }
}
