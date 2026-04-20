package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.cqrs.QueryResult;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;

public record ProductDetailView(ProductVO product) implements QueryResult {
    public static ProductDetailView of(ProductVO product) {
        return new ProductDetailView(product);
    }
}
