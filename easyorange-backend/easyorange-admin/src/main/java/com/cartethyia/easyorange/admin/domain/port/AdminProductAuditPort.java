package com.cartethyia.easyorange.admin.domain.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 模块的商品审核端口。
 * <p>
 * <b>取舍</b>：审核是一次带操作人身份的写操作，所以端口参数里显式带 {@code operatorId} /
 * {@code operatorName} —— 让 admin 侧自己查一次当前用户再传进来，是把鉴权上下文漏过模块边界的另一种写法。
 * 动作与状态一律传 String code，枚举翻译留在 product 侧，admin 侧不复制一份审核状态机。
 * <p>
 * <b>边界</b>：商品是否存在、状态是否允许审核由 product 侧裁决后抛业务异常；本端口只落日志、
 * 发事件，不吞异常也不改写结果。
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

    /** 按时间倒序。 */
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
