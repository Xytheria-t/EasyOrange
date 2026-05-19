package com.cartethyia.easyorange.ai.dto;

import java.util.List;

public record SemanticSearchResult(
        List<?> records,
        long total,
        int pageNum,
        int pageSize
) {
    public static SemanticSearchResult empty(int pageNum, int pageSize) {
        return new SemanticSearchResult(List.of(), 0L, pageNum, pageSize);
    }
}