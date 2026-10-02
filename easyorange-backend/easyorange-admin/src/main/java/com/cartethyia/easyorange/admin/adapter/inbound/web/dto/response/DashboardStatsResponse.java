package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record DashboardStatsResponse(
        Long totalUsers,
        Long todayNewUsers,
        Long totalProducts,
        Long pendingProducts,
        Long totalOrders,
        Long todayOrders,
        BigDecimal totalRevenue) {}
