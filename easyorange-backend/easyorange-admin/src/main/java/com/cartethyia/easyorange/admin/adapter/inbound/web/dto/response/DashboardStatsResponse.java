package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {

    private Long totalUsers;

    private Long todayNewUsers;

    private Long totalProducts;

    private Long pendingProducts;

    private Long totalOrders;

    private Long todayOrders;

    private Long totalRevenue;

    private Long pendingReports;
}