package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;

public class ProductDetail {

    private ProductId productId;
    private ProductDescription description;

    private ProductDetail() {
    }

    public static ProductDetail create(ProductId productId, String description) {
        ProductDetail detail = new ProductDetail();
        detail.productId = productId;
        detail.description = ProductDescription.of(description);
        return detail;
    }

    public ProductId getProductId() {
        return productId;
    }

    public ProductDescription getDescription() {
        return description;
    }

    public void updateDescription(String description) {
        this.description = ProductDescription.of(description);
    }
}
