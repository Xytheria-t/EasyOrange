package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.product.domain.enums.ProductStatus;

public record ProductStatusVO(ProductStatus value) {
    public ProductStatusVO {
        if (value == null) {
            throw new IllegalArgumentException("商品状态不能为空");
        }
    }

    public static ProductStatusVO of(Integer code) {
        return new ProductStatusVO(ProductStatus.fromCode(code));
    }

    public static ProductStatusVO of(ProductStatus status) {
        return new ProductStatusVO(status);
    }

    public Integer code() {
        return value != null ? value.getCode() : null;
    }

    public String desc() {
        return value != null ? value.getDesc() : null;
    }

    public boolean isOnLine() {
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
}