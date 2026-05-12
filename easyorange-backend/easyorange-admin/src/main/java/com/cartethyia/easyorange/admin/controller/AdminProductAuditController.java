package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.dto.response.AuditLogVO;
import com.cartethyia.easyorange.admin.dto.response.BatchAuditResultVO;
import com.cartethyia.easyorange.admin.service.AdminProductAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
class AdminProductAuditController {

    private final AdminProductAuditService adminProductAuditService;

    @PutMapping("/{id}/audit")
    public Result<Void> auditProduct(
        @PathVariable Long id,
        @Valid @RequestBody ProductAuditRequest request
    ) {
        adminProductAuditService.auditProduct(id, request);
        return Result.success();
    }

    @PostMapping("/batch-audit")
    public Result<BatchAuditResultVO> batchAudit(
        @Valid @RequestBody BatchAuditRequest request
    ) {
        return Result.success(adminProductAuditService.batchAudit(request));
    }

    @GetMapping("/{id}/audit-logs")
    public Result<List<AuditLogVO>> getAuditLogs(@PathVariable Long id) {
        return Result.success(adminProductAuditService.getAuditLogs(id));
    }
}
