package com.cartethyia.easyorange.product.application.query;

import java.math.BigDecimal;

/**
 * 商品列表查询参数对象 — 收敛 10 个长参数为单一 record。
 * <p>
 * 提升调用点可读性并避免参数顺序错配；分页参数通过 effective* 方法提供默认值。
 */
public record ProductListQuery(
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
    public int effectivePageNum() {
        return pageNum != null ? pageNum : 1;
    }

    public int effectivePageSize() {
        return pageSize != null ? pageSize : 20;
    }
}
