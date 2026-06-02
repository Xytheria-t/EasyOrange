package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.cartethyia.easyorange.common.dto.AiEnhancement;

import java.util.List;

public record SearchPageResponse<T>(
    List<T> records,
    long total,
    int pageNum,
    int pageSize,
    List<FacetBucketResponse> facets,
    AiEnhancement aiEnhancement
) {
    public static <T> SearchPageResponse<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new SearchPageResponse<>(
            records != null ? records : List.of(),
            total,
            pageNum,
            pageSize,
            List.of(),
            null
        );
    }

    public static <T> SearchPageResponse<T> of(List<T> records, long total, int pageNum, int pageSize,
                                               List<FacetBucketResponse> facets) {
        return new SearchPageResponse<>(
            records != null ? records : List.of(),
            total,
            pageNum,
            pageSize,
            facets != null ? List.copyOf(facets) : List.of(),
            null
        );
    }

    public SearchPageResponse<T> withAiEnhancement(AiEnhancement aiEnhancement) {
        return new SearchPageResponse<>(this.records, this.total, this.pageNum, this.pageSize,
            this.facets, aiEnhancement);
    }
}
