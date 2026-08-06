package com.cartethyia.easyorange.ai.dto;

import java.util.List;

public record SemanticSearchResult(List<?> records, long total, int current, int size) {
    public static SemanticSearchResult empty(int current, int size) {
        return new SemanticSearchResult(List.of(), 0L, current, size);
    }
}
