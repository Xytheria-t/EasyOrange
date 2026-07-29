package com.cartethyia.easyorange.product.application.query;

import java.math.BigDecimal;

/**
 * 商品搜索条件 — 聚合所有查询参数的参数对象。
 * 替代 {@code ProductQueryRepository.searchProducts()} 的 10 个独立参数。
 */
public record ProductSearchCriteria(
    String keyword,
    String categoryId,
    String status,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String conditionLevel,
    String sort,
    Boolean hasDiscount,
    Integer pageNum,
    Integer pageSize
) {
    public ProductSearchCriteria {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
    }

    public int effectivePageNum() { return pageNum; }
    public int effectivePageSize() { return pageSize; }

    public static ProductSearchCriteria byCategory(String categoryId, int pageNum, int pageSize) {
        return new ProductSearchCriteria(null, categoryId, null, null, null, null, null, null, pageNum, pageSize);
    }
}
