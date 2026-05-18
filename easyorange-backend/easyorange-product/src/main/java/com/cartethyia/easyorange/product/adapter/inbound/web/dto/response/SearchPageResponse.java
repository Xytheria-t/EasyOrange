package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.util.List;

public record SearchPageResponse<T>(
    List<T> records,
    long total,
    int pageNum,
    int pageSize,
    List<FacetBucketResponse> facets
) {
    public static <T> SearchPageResponse<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new SearchPageResponse<>(
            records != null ? records : List.of(),
            total,
            pageNum,
            pageSize,
            List.of()
        );
    }

    public static <T> SearchPageResponse<T> of(List<T> records, long total, int pageNum, int pageSize,
                                               List<FacetBucketResponse> facets) {
        return new SearchPageResponse<>(
            records != null ? records : List.of(),
            total,
            pageNum,
            pageSize,
            facets != null ? List.copyOf(facets) : List.of()
        );
    }
}