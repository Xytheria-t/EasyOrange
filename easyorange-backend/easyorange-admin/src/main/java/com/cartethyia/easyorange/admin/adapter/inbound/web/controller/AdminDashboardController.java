package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminDashboardAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.application.service.AdminDashboardAppService;
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

    private final AdminDashboardAppService adminDashboardService;
    private final AdminDashboardAssembler assembler;

    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getStats() {
        return Result.success(assembler.toStatsResponse(adminDashboardService.getDashboardStats()));
    }

    @GetMapping("/trend")
    public Result<List<TrendResponse>> getTrend() {
        return Result.success(assembler.toTrendResponses(adminDashboardService.getTrend()));
    }

    @GetMapping("/activity")
    public Result<List<ActivityResponse>> getActivity() {
        return Result.success(assembler.toActivityResponses(adminDashboardService.getRecentActivity()));
    }
}
