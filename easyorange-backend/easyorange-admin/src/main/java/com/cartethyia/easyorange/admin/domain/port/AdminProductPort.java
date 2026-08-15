package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的商品查询/操作端口
 * 用于跨模块查询与操作商品信息，遵循防腐层原则
 * <p>
 * 审核/举报/分类/仪表板功能域见 {@link AdminProductAuditPort}、{@link AdminReportPort}、
 * {@link AdminCategoryPort}、{@link AdminDashboardPort}
 */
public interface AdminProductPort {

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
     * 根据产品 ID 列表批量查询产品基本信息
     */
    Map<String, ProductInfo> getProductInfos(List<String> productIds);

    /**
     * 管理员直改商品状态（ONLINE/OFFLINE/SOLD），非法状态码/商品不存在/状态转换不允许时抛出 BusinessException
     */
    void applyProductStatus(String productId, String statusCode);

    /**
     * 产品基本信息
     */
    record ProductInfo(String id, String name) {}

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
            Integer pageSize) {}

    /**
     * 产品查询结果
     */
    record ProductQueryResult(List<ProductSummary> records, long total, int pageNum, int pageSize) {}

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
            LocalDateTime updateTime) {}

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
            LocalDateTime updateTime) {}
}
