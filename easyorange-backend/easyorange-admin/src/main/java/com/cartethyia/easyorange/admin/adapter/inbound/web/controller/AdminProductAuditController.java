package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAuditAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.application.service.AdminProductAuditAppService;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditItem;
import com.cartethyia.easyorange.admin.domain.model.ProductAuditCommand;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-审核", description = "商品审核")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductAuditController {

    private final AdminProductAuditAppService adminProductAuditService;
    private final AdminProductAuditAssembler assembler;

    @PutMapping("/{id}/audit")
    public Result<Void> auditProduct(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody ProductAuditRequest request) {
        adminProductAuditService.auditProduct(operator, id, toCommand(request));
        return Result.success();
    }

    @PostMapping("/batch-audit")
    public Result<BatchAuditResultResponse> batchAudit(
            @AuthenticationPrincipal AuthUser operator, @Valid @RequestBody BatchAuditRequest request) {
        List<BatchAuditItem> items = request.items().stream()
                .map(i -> new BatchAuditItem(i.productId(), i.action(), i.reason(), i.dimensions()))
                .toList();
        return Result.success(assembler.toBatchResultResponse(adminProductAuditService.batchAudit(operator, items)));
    }

    @GetMapping("/{id}/audit-logs")
    public Result<List<AuditLogResponse>> getAuditLogs(@PathVariable String id) {
        return Result.success(assembler.toAuditLogResponses(adminProductAuditService.getAuditLogs(id)));
    }

    private static ProductAuditCommand toCommand(ProductAuditRequest request) {
        return new ProductAuditCommand(request.action(), request.reason(), request.remark(), request.dimensions());
    }
}
