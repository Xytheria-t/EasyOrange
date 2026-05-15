package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.admin.dto.response.ActivityVO;
import com.cartethyia.easyorange.admin.dto.response.DashboardStatsVO;
import com.cartethyia.easyorange.admin.dto.response.PendingItemsVO;
import com.cartethyia.easyorange.admin.dto.response.RecentProductVO;
import com.cartethyia.easyorange.admin.dto.response.RecentUserVO;
import com.cartethyia.easyorange.admin.dto.response.TrendVO;
import com.cartethyia.easyorange.admin.service.AdminDashboardService;
import com.cartethyia.easyorange.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> getStats() {
        return Result.success(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/pending")
    public Result<PendingItemsVO> getPendingItems() {
        return Result.success(adminDashboardService.getPendingItems());
    }

    @GetMapping("/recent-users")
    public Result<List<RecentUserVO>> getRecentUsers() {
        return Result.success(adminDashboardService.getRecentUsers(10));
    }

    @GetMapping("/recent-products")
    public Result<List<RecentProductVO>> getRecentProducts() {
        return Result.success(adminDashboardService.getRecentProducts(10));
    }

    @GetMapping("/trend")
    public Result<List<TrendVO>> getTrend() {
        return Result.success(adminDashboardService.getTrend());
    }

    @GetMapping("/activity")
    public Result<List<ActivityVO>> getActivity() {
        return Result.success(adminDashboardService.getRecentActivity());
    }
}
