package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAuditAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.application.service.AdminProductAuditAppService;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "审核单个商品（action 1 通过 / 2 拒绝），记操作人并落审核日志")
    public Result<Void> auditProduct(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody ProductAuditRequest request) {
        adminProductAuditService.auditProduct(operator, id, assembler.toCommand(request));
        return Result.success();
    }

    @PostMapping("/batch-audit")
    @Operation(summary = "批量审核（单次上限 50 条），逐条独立事务，返回部分成功与失败明细")
    public Result<BatchAuditResultResponse> batchAudit(
            @AuthenticationPrincipal AuthUser operator, @Valid @RequestBody BatchAuditRequest request) {
        return Result.success(assembler.toBatchResultResponse(
                adminProductAuditService.batchAudit(operator, assembler.toBatchItems(request))));
    }

    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "某商品的审核流水，按时间倒序，含审核前后状态与命中维度")
    public Result<List<AuditLogResponse>> getAuditLogs(@PathVariable String id) {
        return Result.success(assembler.toAuditLogResponses(adminProductAuditService.getAuditLogs(id)));
    }
}
