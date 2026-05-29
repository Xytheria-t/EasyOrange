package com.cartethyia.easyorange.product.domain.port;

import java.util.List;

public interface ProductSearchQueryPort {

    SearchResult search(ProductSearchQuery query);

    record ProductSearchQuery(
        String keyword,
        Long categoryId,
        Integer status,
        java.math.BigDecimal minPrice,
        java.math.BigDecimal maxPrice,
        Integer conditionLevel,
        String sort,
        int pageNum,
        int pageSize,
        List<Float> queryEmbedding,
        boolean useSemanticSearch
    ) { }
}