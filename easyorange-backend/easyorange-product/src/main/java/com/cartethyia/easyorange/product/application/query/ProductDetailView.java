package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.product.dto.vo.ProductVO;

public record ProductDetailView(ProductVO product) {
    public static ProductDetailView of(ProductVO product) {
        return new ProductDetailView(product);
    }
}
