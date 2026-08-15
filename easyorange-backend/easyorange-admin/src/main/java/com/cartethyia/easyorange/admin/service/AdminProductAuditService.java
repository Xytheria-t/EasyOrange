package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AiReviewRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductAuditService {

    private final AdminProductAuditPort adminProductAuditPort;

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

    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultResponse batchAudit(AuthUser operator, BatchAuditRequest request) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (BatchAuditRequest.AuditItem item : request.getItems()) {
            try {
                adminProductAuditPort.auditProduct(
                        item.productId(),
                        item.action(),
                        item.reason(),
                        null,
                        item.dimensions(),
                        operator.userId(),
                        operator.username());
                successCount++;
            } catch (Exception e) {
                errors.add("商品ID " + item.productId() + ": " + e.getMessage());
            }
        }

        return new BatchAuditResultResponse(request.getItems().size(), successCount, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String productId) {
        return adminProductAuditPort.getAuditLogs(productId).stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    public AiReviewRecord getAiReview(String productId) {
        return adminProductAuditPort.getAiReview(productId);
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
