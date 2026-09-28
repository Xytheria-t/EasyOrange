package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 商品审核出参组装 — 端口给的是 String code，前端要的是数字动作值，转换在这一层收口。
 * <p>
 * {@code beforeStatus} / {@code afterStatus} 保持字符串：它们是 order/product 侧的状态码原文，
 * 数值化要在展示层另定映射表，凭空造一份数字枚举只会多一处可能对不上的真相。
 */
@Component
public class AdminProductAuditAssembler {

    public List<AuditLogResponse> toAuditLogResponses(List<AuditLogRecord> logs) {
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    public BatchAuditResultResponse toBatchResultResponse(BatchAuditResult result) {
        return new BatchAuditResultResponse(
                result.total(), result.success(), result.errors().size(), result.errors());
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
