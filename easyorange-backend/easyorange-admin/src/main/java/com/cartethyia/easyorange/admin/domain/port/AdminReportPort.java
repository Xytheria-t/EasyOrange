package com.cartethyia.easyorange.admin.domain.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 模块的举报查询/处理端口
 * 用于跨模块查询与处理举报信息，遵循防腐层原则
 */
public interface AdminReportPort {

    /**
     * 分页查询举报列表
     */
    ReportQueryResult queryReports(Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询举报详情，不存在时返回 null
     */
    ReportRecord getReportDetail(String reportId);

    /**
     * 查询举报处理历史（按时间倒序）
     */
    List<ReportHistoryRecord> getReportHistory(String reportId);

    /**
     * 举报状态统计
     */
    ReportStats getReportStats();

    /**
     * 处理单条举报：校验待处理状态、执行商品下线/封禁副作用、记录处理历史并发布领域事件
     */
    void handleReport(String reportId, String actionCode, String remark, String operatorId);

    /**
     * 举报查询结果
     */
    record ReportQueryResult(List<ReportRecord> records, long total, int pageNum, int pageSize) {}

    /**
     * 举报记录 — status/reasonType 为 String code，desc 已解析
     */
    record ReportRecord(
            String id,
            String productId,
            String reporterId,
            String reasonType,
            String reasonTypeDesc,
            String reason,
            String status,
            String statusDesc,
            String remark,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            boolean pending) {}

    /**
     * 举报处理历史记录
     */
    record ReportHistoryRecord(
            String id, String reportId, String operatorId, String action, String remark, LocalDateTime createTime) {}

    /**
     * 举报状态统计
     */
    record ReportStats(long total, long pending, long processing, long resolved, long dismissed) {}
}
