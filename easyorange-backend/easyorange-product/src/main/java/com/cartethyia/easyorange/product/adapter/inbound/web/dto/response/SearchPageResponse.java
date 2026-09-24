package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.util.List;

public record SearchPageResponse<T>(
        List<T> records, long total, int current, int size, int pages, List<FacetBucketResponse> facets) {}
