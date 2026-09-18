package com.cartethyia.easyorange.product.application.port.query;

import java.util.List;

public interface ProductSearchQueryPort {

    SearchResult search(ProductSearchQuery query);

    /**
     * 「按相关性排序」判定 —— 两路召回（BM25 + kNN）能否生效的唯一前提。
     * <p>
     * 用户显式点了价格 / 最新 / 热度时，排序语义压过相关性：融合出来的是相关性排名，
     * 拿它当结果顺序会和用户点选打架。调用方（应用层决定要不要付 embedding 调用、适配器决定要不要走融合）
     * 共用这一处判定，避免两边各写一份而漂移。
     */
    static boolean isRelevanceSort(String sort) {
        return sort == null || sort.isBlank() || "relevance".equals(sort);
    }

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
