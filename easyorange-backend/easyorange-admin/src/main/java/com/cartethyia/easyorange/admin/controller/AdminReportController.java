package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.dto.request.ReportHandleRequest;
import com.cartethyia.easyorange.admin.dto.request.BatchHandleRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminReportVO;
import com.cartethyia.easyorange.admin.dto.response.ReportStatsVO;
import com.cartethyia.easyorange.admin.dto.response.ReportHandleHistoryVO;
import com.cartethyia.easyorange.admin.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public Result<PageResult<AdminReportVO>> listReports(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) Integer status
    ) {
        return Result.success(adminReportService.listReports(pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    public Result<AdminReportVO> getReportDetail(@PathVariable Long id) {
        return Result.success(adminReportService.getReportDetail(id));
    }

    @GetMapping("/{id}/history")
    public Result<List<ReportHandleHistoryVO>> getReportHistory(@PathVariable Long id) {
        return Result.success(adminReportService.getReportHistory(id));
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handleReport(
        @PathVariable Long id,
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
    public Result<ReportStatsVO> getReportStats() {
        return Result.success(adminReportService.getReportStats());
    }
}
