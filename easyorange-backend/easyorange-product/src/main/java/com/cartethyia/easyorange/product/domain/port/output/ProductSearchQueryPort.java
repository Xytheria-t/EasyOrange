package com.cartethyia.easyorange.product.domain.port.output;

import java.util.List;

/**
 * 商品搜索查询端口 — ES 实现此接口，不可用时降级到 ProductQueryRepository.
 */
public interface ProductSearchQueryPort extends OutboundPort {

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
