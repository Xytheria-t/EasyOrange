package com.cartethyia.easyorange.product.application.port.query;

import java.util.List;

public interface ProductSearchQueryPort {

    SearchResult search(ProductSearchQuery query);

    record ProductSearchQuery(
            String keyword,
            String categoryId,
            String status,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            String conditionLevel,
            String sort,
            int pageNum,
            int pageSize,
            List<Float> queryEmbedding,
            boolean useSemanticSearch) {}
}
