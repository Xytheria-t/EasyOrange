package com.cartethyia.easyorange.product.application.query.readmodel;

public record HotKeywordReadModel(
    String id,
    String keyword,
    Integer searchCount,
    Integer hotLevel
) { }
