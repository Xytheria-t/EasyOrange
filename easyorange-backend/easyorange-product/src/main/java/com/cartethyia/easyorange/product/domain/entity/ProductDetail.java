package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;

public record ProductDetail(
        ProductId productId,
        ProductDescription description
) {

    public static ProductDetail create(ProductId productId, String description) {
        return new ProductDetail(productId, ProductDescription.of(description));
    }

    public ProductDetail withDescription(String description) {
        return new ProductDetail(this.productId, ProductDescription.of(description));
    }
}
