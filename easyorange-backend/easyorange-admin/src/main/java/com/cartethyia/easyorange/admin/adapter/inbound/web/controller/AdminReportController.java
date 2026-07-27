package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportStatsResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.service.AdminReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台-举报", description = "举报处理")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public Result<PageResult<AdminReportResponse>> listReports(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) Integer status
    ) {
        return Result.success(adminReportService.listReports(pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    public Result<AdminReportResponse> getReportDetail(@PathVariable String id) {
        return Result.success(adminReportService.getReportDetail(id));
    }

    @GetMapping("/{id}/history")
    public Result<List<ReportHandleHistoryResponse>> getReportHistory(@PathVariable String id) {
        return Result.success(adminReportService.getReportHistory(id));
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handleReport(
        @PathVariable String id,
        @Valid @RequestBody ReportHandleRequest request
    ) {
        adminReportService.handleReport(id, request);
        return Result.success();
    }

    @PutMapping("/batch-handle")
    public Result<Void> batchHandleReports(@Valid @RequestBody BatchHandleRequest request) {
        adminReportService.batchHandleReports(request);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<ReportStatsResponse> getReportStats() {
        return Result.success(adminReportService.getReportStats());
    }
}