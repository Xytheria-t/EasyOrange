package com.cartethyia.easyorange.product.application.port.query;

public record FacetBucket(
    String key,
    String label,
    long count
) { }