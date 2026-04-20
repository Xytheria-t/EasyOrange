package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.product.enums.ProductStatus;

import java.util.Objects;

public final class ProductStatusVO implements ValueObject {

    private final ProductStatus value;

    public ProductStatusVO(ProductStatus value) {
        this.value = Objects.requireNonNull(value, "商品状态不能为空");
    }

    public ProductStatus value() {
        return value;
    }

    public boolean isOnline() {
        return ProductStatus.ONLINE.equals(value);
    }

    public boolean isSold() {
        return ProductStatus.SOLD.equals(value);
    }

    public boolean isDraft() {
        return ProductStatus.DRAFT.equals(value);
    }

    public boolean isOffline() {
        return ProductStatus.OFFLINE.equals(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStatusVO that = (ProductStatusVO) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ProductStatusVO{" + value + '}';
    }
}
