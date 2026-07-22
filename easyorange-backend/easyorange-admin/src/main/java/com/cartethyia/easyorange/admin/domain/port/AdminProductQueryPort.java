package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的产品查询端口
 * 用于跨模块查询产品信息，遵循防腐层原则
 */
public interface AdminProductQueryPort {

    /**
     * 查询产品列表（带条件查询）
     */
    ProductQueryResult queryProducts(ProductQueryCondition condition);

    /**
     * 根据产品 ID 查询产品详情
     */
    ProductDetail getProductDetail(String productId);

    /**
     * 根据产品 ID 列表批量查询产品图片
     */
    Map<String, List<String>> getProductImages(List<String> productIds);

    /**
     * 产品查询条件
     */
    record ProductQueryCondition(
        String keyword,
        String categoryId,
        String status,
        String sellerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer pageNum,
        Integer pageSize
    ) {}

    /**
     * 产品查询结果
     */
    record ProductQueryResult(
        List<ProductSummary> records,
        long total,
        int pageNum,
        int pageSize
    ) {}

    /**
     * 产品摘要信息
     */
    record ProductSummary(
        String id,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String status,
        String statusDesc,
        String conditionLevel,
        String location,
        String contactMethod,
        String categoryId,
        String sellerId,
        Integer viewCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
    ) {}

    /**
     * 产品详情信息
     */
    record ProductDetail(
        String id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String status,
        String statusDesc,
        String conditionLevel,
        String location,
        String contactMethod,
        String categoryId,
        String sellerId,
        Integer viewCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
    ) {}
}