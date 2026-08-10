package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 模块的商品审核端口
 * 用于跨模块执行商品审核、查询审核日志与 AI 预审，遵循防腐层原则
 */
public interface AdminProductAuditQueryPort {

    /**
     * 获取 AI 审核所需数据（跨表读），产品不存在或已删除时返回 null
     */
    AiReviewData getAiReviewData(String productId);

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
     * AI 预审（调用 AI 审核服务），产品不存在或已删除时抛出 ProductNotFoundException
     */
    AiReviewRecord getAiReview(String productId);

    /**
     * AI 审核数据
     */
    record AiReviewData(
            String name,
            String description,
            String categoryName,
            String conditionLevel,
            BigDecimal price,
            String sellerName,
            List<String> imageUrls) {}

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

    /**
     * AI 审核结果
     */
    record AiReviewRecord(
            boolean isApproved,
            String suggestedActionDesc,
            double confidenceScore,
            List<String> riskFlags,
            String reasoning) {}
}
