package com.cartethyia.easyorange.product.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.dto.request.ReportRequest;
import com.cartethyia.easyorange.product.entity.ProductReport;
import com.cartethyia.easyorange.product.service.ProductReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductReportService productReportService;

    @PostMapping("/product/{productId}")
    public Result<Void> reportProduct(@PathVariable Long productId,
                                      @Valid @RequestBody ReportRequest request) {
        Long reporterId = SecurityContextUtil.getCurrentUserIdOrThrow();
        productReportService.reportProduct(productId, reporterId, request.getReason());
        return Result.success();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<PageResult<ProductReport>> getPendingReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<ProductReport> result = productReportService.getPendingReports(pageNum, pageSize);
        return Result.success(result);
    }

    @PutMapping("/{reportId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Void> approveReport(@PathVariable Long reportId) {
        productReportService.processReport(reportId, true);
        return Result.success();
    }

    @PutMapping("/{reportId}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Void> rejectReport(@PathVariable Long reportId) {
        productReportService.processReport(reportId, false);
        return Result.success();
    }
}
