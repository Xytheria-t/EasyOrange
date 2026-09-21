package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.service.AdminDashboardService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/trend")
    public Result<List<TrendResponse>> getTrend() {
        return Result.success(adminDashboardService.getTrend());
    }

    @GetMapping("/activity")
    public Result<List<ActivityResponse>> getActivity() {
        return Result.success(adminDashboardService.getRecentActivity());
    }
}
