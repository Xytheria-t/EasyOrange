package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.product.enums.ProductStatus;

public record ProductStatusVO(ProductStatus value) {
    public ProductStatusVO {
        if (value == null) {
            throw new IllegalArgumentException("商品状态不能为空");
        }
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
}