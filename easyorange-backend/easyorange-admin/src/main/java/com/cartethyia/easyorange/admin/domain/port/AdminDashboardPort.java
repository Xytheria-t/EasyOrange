package com.cartethyia.easyorange.admin.domain.port;

/**
 * Admin 模块的仪表板查询端口
 * 用于跨模块聚合商品统计与榜单数据，遵循防腐层原则
 */
public interface AdminDashboardPort {

    /**
     * 商品统计：总数与待审核（DRAFT）数
     */
    ProductStats getProductStats();

    /**
     * 商品统计
     */
    record ProductStats(long total, long pending) {}
}
