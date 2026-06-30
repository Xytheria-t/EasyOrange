package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.service.AdminProductAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductAuditController {

    private final AdminProductAuditService adminProductAuditService;

    @PutMapping("/{id}/audit")
    public Result<Void> auditProduct(
        @PathVariable String id,
        @Valid @RequestBody ProductAuditRequest request
    ) {
        adminProductAuditService.auditProduct(id, request);
        return Result.success();
    }

    @PostMapping("/batch-audit")
    public Result<BatchAuditResultResponse> batchAudit(
        @Valid @RequestBody BatchAuditRequest request
    ) {
        return Result.success(adminProductAuditService.batchAudit(request));
    }

    @GetMapping("/{id}/audit-logs")
    public Result<List<AuditLogResponse>> getAuditLogs(@PathVariable String id) {
        return Result.success(adminProductAuditService.getAuditLogs(id));
    }

    @GetMapping("/{id}/ai-review")
    public Result<AiReviewResult> getAiReview(@PathVariable String id) {
        return Result.success(adminProductAuditService.getAiReview(id));
    }
}