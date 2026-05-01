package com.cartethyia.easyorange.product.application.query.readmodel;

public record HotKeywordReadModel(
    Long id,
    String keyword,
    Integer searchCount,
    Integer hotLevel
) { }
