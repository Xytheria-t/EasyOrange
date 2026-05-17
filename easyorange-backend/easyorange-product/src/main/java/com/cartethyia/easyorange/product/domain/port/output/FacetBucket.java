package com.cartethyia.easyorange.product.domain.port.output;

public record FacetBucket(
    String key,
    String label,
    long count
) { }
