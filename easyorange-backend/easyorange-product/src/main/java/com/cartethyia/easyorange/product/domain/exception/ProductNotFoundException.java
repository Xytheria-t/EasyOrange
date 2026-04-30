package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.product.domain.valueobject.ProductId;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(ProductId id) {
        super("商品不存在: id=" + (id != null ? id.value() : "null"));
    }

    public ProductNotFoundException(Long id) {
        super("商品不存在: id=" + id);
    }
}
