package com.cartethyia.easyorange.controller.admin;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.controller.admin.dto.response.DashboardStatsVO;
import com.cartethyia.easyorange.controller.admin.dto.response.PendingItemsVO;
import com.cartethyia.easyorange.controller.admin.dto.response.RecentProductVO;
import com.cartethyia.easyorange.controller.admin.dto.response.RecentUserVO;
import com.cartethyia.easyorange.controller.admin.service.AdminDashboardService;
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
}
