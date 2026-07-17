package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.Idempotent;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.command.ProductReportCommandService;
import com.cartethyia.easyorange.product.application.query.ProductReportQueryService;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ReportRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductReportCommandService reportCommandService;
    private final ProductReportQueryService reportQueryService;

    @PostMapping("/product/{productId}")
    @Idempotent
    public Result<Void> reportProduct(@PathVariable String productId,
                                       @Valid @RequestBody ReportRequest request) {
        String reporterId = SecurityContextUtil.getCurrentUserIdOrThrow();
        reportCommandService.handleReport(productId, reporterId, request.getReason(), request.getReasonType());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<PageResult<ProductReportResponse>> myReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        String reporterId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(reportQueryService.getMyReports(reporterId, pageNum, pageSize));
    }

    @GetMapping("/{reportId}")
    public Result<ProductReportDetailResponse> getReportDetail(@PathVariable String reportId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(reportQueryService.getReportDetail(reportId, userId));
    }
}
