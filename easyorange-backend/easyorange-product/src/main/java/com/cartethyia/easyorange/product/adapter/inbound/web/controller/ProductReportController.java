package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ReportRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import com.cartethyia.easyorange.product.application.command.ProductReportCommandHandler;
import com.cartethyia.easyorange.product.application.query.ProductReportQueryHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Tag(name = "举报反馈", description = "商品举报")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductReportCommandHandler reportCommandHandler;
    private final ProductReportQueryHandler reportQueryHandler;

    @PostMapping("/product/{productId}")
    public Result<Void> reportProduct(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String productId,
            @Valid @RequestBody ReportRequest request) {
        reportCommandHandler.handleReport(productId, user.userId(), request.reason(), request.reasonType());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<PageResult<ProductReportResponse>> myReports(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(reportQueryHandler.getMyReports(user.userId(), pageNum, pageSize));
    }

    @GetMapping("/{reportId}")
    public Result<ProductReportDetailResponse> getReportDetail(
            @AuthenticationPrincipal AuthUser user, @PathVariable String reportId) {
        return Result.success(reportQueryHandler.getReportDetail(reportId, user.userId()));
    }
}
