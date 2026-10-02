package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditItem;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditResult;
import com.cartethyia.easyorange.admin.domain.model.ProductAuditCommand;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 商品审核出入参组装 — 端口给的是 String code，前端要的是数字动作值，转换在这一层收口。
 * <p>
 * {@code beforeStatus} / {@code afterStatus} 保持字符串：它们是 order/product 侧的状态码原文，
 * 数值化要在展示层另定映射表，凭空造一份数字枚举只会多一处可能对不上的真相。
 * <p>
 * <b>入参转换也在这里</b>：Controller 只做参数校验与路由，DTO → 领域命令的翻译不进Web 层。
 */
@Component
public class AdminProductAuditAssembler {

    public ProductAuditCommand toCommand(ProductAuditRequest request) {
        return new ProductAuditCommand(request.action(), request.reason(), request.remark(), request.dimensions());
    }

    public List<BatchAuditItem> toBatchItems(BatchAuditRequest request) {
        return request.items().stream()
                .map(i -> new BatchAuditItem(i.productId(), i.action(), i.reason(), i.dimensions()))
                .toList();
    }

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
