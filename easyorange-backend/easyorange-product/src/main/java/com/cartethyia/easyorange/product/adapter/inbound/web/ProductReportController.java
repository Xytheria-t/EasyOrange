package com.cartethyia.easyorange.product.adapter.inbound.web;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.command.handler.CreateProductReportHandler;
import com.cartethyia.easyorange.product.application.command.handler.ProcessProductReportHandler;
import com.cartethyia.easyorange.product.application.query.handler.GetPendingReportsHandler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ReportRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProductReportController {

    private final CreateProductReportHandler createProductReportHandler;
    private final GetPendingReportsHandler getPendingReportsHandler;
    private final ProcessProductReportHandler processProductReportHandler;

    @PostMapping("/product/{productId}")
    public Result<Void> reportProduct(@PathVariable Long productId,
                                      @Valid @RequestBody ReportRequest request) {
        Long reporterId = SecurityContextUtil.getCurrentUserIdOrThrow();
        createProductReportHandler.handleReport(productId, reporterId, request.getReason());
        return Result.success();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<com.cartethyia.easyorange.common.result.PageResult<ProductReportResponse>> getPendingReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(getPendingReportsHandler.handle(pageNum, pageSize));
    }

    @PutMapping("/{reportId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Void> approveReport(@PathVariable Long reportId) {
        processProductReportHandler.handleApprove(reportId);
        return Result.success();
    }

    @PutMapping("/{reportId}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Void> rejectReport(@PathVariable Long reportId) {
        processProductReportHandler.handleReject(reportId);
        return Result.success();
    }
}
