package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.model.BatchAuditItem;
import com.cartethyia.easyorange.admin.domain.model.BatchAuditResult;
import com.cartethyia.easyorange.admin.domain.model.ProductAuditCommand;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditPort.AuditLogRecord;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 后台商品审核 — 审核动作与审核日志的编排，命令与结果都是 {@code domain} 记录。
 * <p>
 * <b>取舍</b>：动作码、原因、备注、命中维度打成一个 {@link ProductAuditCommand}，
 * 单条与批量因此共用同一套到端口的参数映射，两条入口不会各自漂移。
 * <p>
 * <b>边界</b>：审核是否合法由 product 侧的聚合裁决，本类不预判；日志里的状态码保持原文，数值化在 web 层。
 */
@Service
@RequiredArgsConstructor
public class AdminProductAuditAppService {

    private final AdminProductAuditPort adminProductAuditPort;
    private final TransactionTemplate transactionTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(AuthUser operator, String id, ProductAuditCommand command) {
        adminProductAuditPort.auditProduct(
                id,
                command.action(),
                command.reason(),
                command.remark(),
                command.dimensions(),
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
    public BatchAuditResult batchAudit(AuthUser operator, List<BatchAuditItem> items) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (BatchAuditItem item : items) {
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

        return new BatchAuditResult(items.size(), successCount, errors);
    }

    @Transactional(readOnly = true)
    public List<AuditLogRecord> getAuditLogs(String productId) {
        return adminProductAuditPort.getAuditLogs(productId);
    }
}
