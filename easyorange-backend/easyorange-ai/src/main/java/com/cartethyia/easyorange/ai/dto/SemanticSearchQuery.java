package com.cartethyia.easyorange.ai.dto;

public record SemanticSearchQuery(
        String keyword,
        int pageNum,
        int pageSize,
        String categoryFilter
) {
}