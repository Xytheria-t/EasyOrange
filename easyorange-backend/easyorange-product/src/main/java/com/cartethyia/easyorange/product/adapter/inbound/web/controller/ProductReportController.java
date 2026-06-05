package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.command.handler.CreateProductReportHandler;
import com.cartethyia.easyorange.product.application.query.handler.GetMyReportsHandler;
import com.cartethyia.easyorange.product.application.query.handler.GetReportDetailHandler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ReportRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProductReportController {

    private final CreateProductReportHandler createProductReportHandler;
    private final GetMyReportsHandler getMyReportsHandler;
    private final GetReportDetailHandler getReportDetailHandler;

    @PostMapping("/product/{productId}")
    public Result<Void> reportProduct(@PathVariable Long productId,
                                       @Valid @RequestBody ReportRequest request) {
        Long reporterId = SecurityContextUtil.getCurrentUserIdOrThrow();
        createProductReportHandler.handleReport(productId, reporterId, request.getReason(), request.getReasonType());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<PageResult<ProductReportResponse>> myReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long reporterId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(getMyReportsHandler.handle(reporterId, pageNum, pageSize));
    }

    @GetMapping("/{reportId}")
    public Result<ProductReportDetailResponse> getReportDetail(@PathVariable Long reportId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return Result.success(getReportDetailHandler.handle(reportId, userId));
    }
}
