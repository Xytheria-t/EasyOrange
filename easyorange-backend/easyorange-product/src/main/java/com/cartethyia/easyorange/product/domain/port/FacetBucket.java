package com.cartethyia.easyorange.product.domain.port;

public record FacetBucket(
    String key,
    String label,
    long count
) { }