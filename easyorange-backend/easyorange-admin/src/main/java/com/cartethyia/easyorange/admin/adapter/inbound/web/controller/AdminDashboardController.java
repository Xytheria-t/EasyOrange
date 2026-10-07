package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminDashboardAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ActivityResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.DashboardStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.TrendResponse;
import com.cartethyia.easyorange.admin.application.service.AdminDashboardAppService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "用户 / 商品 / 订单三模块总量与今日增量，含待审商品数与累计营收")
    public Result<DashboardStatsResponse> getStats() {
        return Result.success(assembler.toStatsResponse(adminDashboardService.getDashboardStats()));
    }

    @GetMapping("/trend")
    @Operation(summary = "自 6 个月前起逐自然月补零：新增用户 / 商品 / 订单三条折线")
    public Result<List<TrendResponse>> getTrend() {
        return Result.success(assembler.toTrendResponses(adminDashboardService.getTrend()));
    }

    @GetMapping("/activity")
    @Operation(summary = "注册 / 上架 / 下单动态合并倒序，每类各取 5 条、合计上限 10 条")
    public Result<List<ActivityResponse>> getActivity() {
        return Result.success(assembler.toActivityResponses(adminDashboardService.getRecentActivity()));
    }
}
