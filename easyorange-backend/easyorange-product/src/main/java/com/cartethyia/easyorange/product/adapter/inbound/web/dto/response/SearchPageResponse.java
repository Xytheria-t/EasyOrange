package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;

public record SearchPageResponse<T>(
        List<T> records,
        long total,
        int current,
        int size,
        int pages,
        List<FacetBucketResponse> facets,
        AiEnhancement aiEnhancement) {
    public static <T> SearchPageResponse<T> of(List<T> records, long total, int current, int size) {
        int pages = PageResult.calcPages(total, size);
        return new SearchPageResponse<>(
                records != null ? records : List.of(), total, current, size, pages, List.of(), null);
    }

    public static <T> SearchPageResponse<T> of(
            List<T> records, long total, int current, int size, List<FacetBucketResponse> facets) {
        int pages = PageResult.calcPages(total, size);
        return new SearchPageResponse<>(
                records != null ? records : List.of(),
                total,
                current,
                size,
                pages,
                facets != null ? List.copyOf(facets) : List.of(),
                null);
    }

    public static <T> SearchPageResponse<T> of(PageResult<?> page, List<T> records) {
        return new SearchPageResponse<>(
                records, page.total(), page.current(), page.size(), page.pages(), List.of(), null);
    }

    public SearchPageResponse<T> withAiEnhancement(AiEnhancement aiEnhancement) {
        return new SearchPageResponse<>(
                this.records, this.total, this.current, this.size, this.pages, this.facets, aiEnhancement);
    }
}
