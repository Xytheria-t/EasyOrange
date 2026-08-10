package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 模块的仪表板查询端口
 * 用于跨模块聚合商品统计与榜单数据，遵循防腐层原则
 */
public interface AdminDashboardQueryPort {

    /**
     * 商品统计：总数与待审核（DRAFT）数
     */
    ProductStats getProductStats();

    /**
     * 最近发布的商品（按创建时间倒序，limit 条）
     */
    List<RecentProductRecord> getRecentProducts(int limit);

    /**
     * 浏览量 Top 商品（仅 ONLINE，limit 条）
     */
    List<TopProductRecord> getTopProducts(int limit);

    /**
     * 商品统计
     */
    record ProductStats(long total, long pending) {}

    /**
     * 最近商品记录
     */
    record RecentProductRecord(
            String id,
            String sellerId,
            String title,
            BigDecimal price,
            String mainImageUrl,
            String status,
            String statusDesc,
            String sellerName,
            String categoryName,
            Integer views,
            LocalDateTime createTime) {}

    /**
     * Top 商品记录
     */
    record TopProductRecord(
            String productId,
            String name,
            Integer viewCount,
            BigDecimal price,
            String mainImage,
            String status,
            String statusDesc) {}
}
