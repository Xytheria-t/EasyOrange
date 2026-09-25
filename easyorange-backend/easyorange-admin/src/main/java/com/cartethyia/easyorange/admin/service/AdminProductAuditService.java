package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductAuditService {

    private final AdminProductAuditPort adminProductAuditPort;
    private final TransactionTemplate transactionTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(AuthUser operator, String id, ProductAuditRequest request) {
        adminProductAuditPort.auditProduct(
                id,
                request.action(),
                request.reason(),
                request.remark(),
                request.dimensions(),
                operator.userId(),
                operator.username());
    }

    /**
     * 批量审核 — 逐条独立事务提交（REQUIRES_NEW 语义用 {@link TransactionTemplate} 表达）：
     * 单条失败不影响已成功条目，「部分成功 + 失败明细」是明确语义。
     * <p>
     * 不用「整体一个事务 + 循环 catch」：那条路依赖「内层永远没有自己的事务」这一巧合——
     * 内层一旦加 {@code @Transactional}，单条失败会把外层事务标记 rollback-only，
     * 全部已成功条目陪葬回滚，响应却仍是部分成功（假成功）。批量条目间无原子性需求。
     */
    public BatchAuditResultResponse batchAudit(AuthUser operator, BatchAuditRequest request) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (BatchAuditRequest.AuditItem item : request.items()) {
            try {
                transactionTemplate.executeWithoutResult(status -> adminProductAuditPort.auditProduct(
                        item.productId(),
                        item.action(),
                        item.reason(),
                        null,
                        item.dimensions(),
                        operator.userId(),
                        operator.username()));
                successCount++;
            } catch (Exception e) {
                errors.add("商品ID " + item.productId() + ": " + e.getMessage());
            }
        }

        return new BatchAuditResultResponse(request.items().size(), successCount, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String productId) {
        return adminProductAuditPort.getAuditLogs(productId).stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    private AuditLogResponse toAuditLogResponse(AuditLogRecord log) {
        return new AuditLogResponse(
                log.id(),
                log.productId(),
                log.operatorId(),
                log.operatorName(),
                Integer.valueOf(log.action()),
                log.actionDesc(),
                log.reason(),
                log.dimensions(),
                log.beforeStatus(),
                log.beforeStatusDesc(),
                log.afterStatus(),
                log.afterStatusDesc(),
                log.remark(),
                log.createTime());
    }
}
