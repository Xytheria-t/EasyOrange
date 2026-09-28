package com.cartethyia.easyorange.admin.domain.port;

import com.cartethyia.easyorange.admin.domain.model.RecentActivity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的商品查询/操作端口 — 跨模块查询与操作商品信息的唯一出口。
 * <p>
 * 审核 / 分类功能域见 {@link AdminProductAuditPort}、{@link AdminCategoryPort}。
 * <p>
 * <b>边界</b>：状态转换合法性由 product 侧的聚合根守卫裁决，端口只负责把 admin 的状态码
 * 翻成领域枚举 —— 端口里不复制一份状态机。
 */
public interface AdminProductPort {

    ProductQueryResult queryProducts(ProductQueryCondition condition);

    ProductDetail getProductDetail(String productId);

    Map<String, List<String>> getProductImages(List<String> productIds);

    Map<String, ProductInfo> getProductInfos(List<String> productIds);

    /**
     * 管理员直改商品状态（ONLINE/OFFLINE/SOLD），非法状态码/商品不存在/状态转换不允许时抛出 BusinessException
     */
    void applyProductStatus(String productId, String statusCode);

    /**
     * 发布趋势：{@code yyyy-MM} → 新增商品数，键按创建时间升序
     */
    Map<String, Long> getCreateTrend(LocalDate since);

    /**
     * 最近发布的商品（按创建时间倒序取 limit 条）
     */
    List<RecentActivity> findRecentPublished(int limit);

    record ProductInfo(String id, String name) {}

    record ProductQueryCondition(
            String keyword,
            String categoryId,
            String status,
            String sellerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    record ProductQueryResult(List<ProductSummary> records, long total, int pageNum, int pageSize) {}

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
