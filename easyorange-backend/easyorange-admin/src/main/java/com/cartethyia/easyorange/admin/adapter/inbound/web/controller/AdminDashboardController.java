package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.PendingItemsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.RecentProductResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.RecentUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TopProductResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.UserActivityHeatmapResponse;
import com.cartethyia.easyorange.admin.service.AdminDashboardService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理后台-仪表盘", description = "运营数据看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getStats() {
        return Result.success(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/pending")
    public Result<PendingItemsResponse> getPendingItems() {
        return Result.success(adminDashboardService.getPendingItems());
    }

    @GetMapping("/recent-users")
    public Result<List<RecentUserResponse>> getRecentUsers() {
        return Result.success(adminDashboardService.getRecentUsers(10));
    }

    @GetMapping("/recent-products")
    public Result<List<RecentProductResponse>> getRecentProducts() {
        return Result.success(adminDashboardService.getRecentProducts(10));
    }

    @GetMapping("/trend")
    public Result<List<TrendResponse>> getTrend() {
        return Result.success(adminDashboardService.getTrend());
    }

    @GetMapping("/activity")
    public Result<List<ActivityResponse>> getActivity() {
        return Result.success(adminDashboardService.getRecentActivity());
    }

    @GetMapping("/user-activity-heatmap")
    public Result<List<UserActivityHeatmapResponse>> getUserActivityHeatmap() {
        return Result.success(adminDashboardService.getUserActivityHeatmap());
    }

    @GetMapping("/top-products")
    public Result<List<TopProductResponse>> getTopProducts(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(adminDashboardService.getTopProducts(limit));
    }
}