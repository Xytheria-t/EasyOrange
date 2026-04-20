package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class ProductDescription implements ValueObject {

    private final String value;

    public ProductDescription(String value) {
        if (value != null && !value.isBlank()) {
            BizRequire.isTrue(value.length() <= 5000, "商品描述不能超过5000个字符");
            this.value = value.trim();
        } else {
            this.value = null;
        }
    }

    public String value() {
        return value;
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDescription that = (ProductDescription) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ProductDescription{" + value + '}';
    }
}
