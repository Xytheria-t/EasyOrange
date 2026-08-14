package com.cartethyia.easyorange.product.domain.valueobject;

/** 待落库的浏览量计数（Redis 缓冲 → DB 批量自增）。 */
public record ViewCountEntry(String productId, int count) {}
