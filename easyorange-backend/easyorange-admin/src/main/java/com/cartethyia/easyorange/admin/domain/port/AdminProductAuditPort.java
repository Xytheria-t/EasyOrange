package com.cartethyia.easyorange.admin.domain.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 模块的商品审核端口
 * 用于跨模块执行商品审核与查询审核日志，遵循防腐层原则
 */
public interface AdminProductAuditPort {

    /**
     * 执行商品审核（actionCode 1 通过 / 2 拒绝），持久化审核日志并发布领域事件
     */
    void auditProduct(
            String productId,
            Integer actionCode,
            String reason,
            String remark,
            List<String> dimensions,
            String operatorId,
            String operatorName);

    /**
     * 查询商品审核日志（按时间倒序）
     */
    List<AuditLogRecord> getAuditLogs(String productId);

    /**
     * 审核日志记录 — action 为 String code（'1' 通过 / '2' 拒绝 / '3' 重提交），desc 已解析
     */
    record AuditLogRecord(
            String id,
            String productId,
            String operatorId,
            String operatorName,
            String action,
            String actionDesc,
            String reason,
            List<String> dimensions,
            String beforeStatus,
            String beforeStatusDesc,
            String afterStatus,
            String afterStatusDesc,
            String remark,
            LocalDateTime createTime) {}
}
