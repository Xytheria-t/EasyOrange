package com.cartethyia.easyorange.admin.domain.model;

import java.math.BigDecimal;

/**
 * 仪表板顶部指标 — 三个模块的统计拼成一行，数值本身没有归属方，故放 admin 的视图模型里。
 *
 * @param totalUsers     用户总数
 * @param todayNewUsers  今日新增用户
 * @param totalProducts  商品总数
 * @param pendingProducts 待审核商品数
 * @param totalOrders    订单总数
 * @param todayOrders    今日订单数
 * @param totalRevenue   累计营收
 */
public record DashboardStats(
        long totalUsers,
        long todayNewUsers,
        long totalProducts,
        long pendingProducts,
        long totalOrders,
        long todayOrders,
        BigDecimal totalRevenue) {}
