package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ReportHandleHistory {

    private final String id;
    private final String reportId;
    private final String operatorId;
    private final String action;
    private final String remark;
    private final LocalDateTime createTime;

    private ReportHandleHistory(
            String id, String reportId, String operatorId, String action, String remark, LocalDateTime createTime) {
        this.id = id;
        this.reportId = reportId;
        this.operatorId = operatorId;
        this.action = action;
        this.remark = remark;
        this.createTime = createTime;
    }

    /**
     * 创建一条举报处置历史。
     *
     * @param id 历史 ID，由调用方（应用层 / 出站适配器）经 {@code IdGenerator} 生成
     *           （{@code BaseDO.id} 为 {@code IdType.INPUT}，数据库不回填）
     */
    public static ReportHandleHistory create(
            String id, String reportId, String operatorId, String action, String remark) {
        if (reportId == null) {
            throw ProductDomainException.reportError("举报ID不能为空");
        }
        if (operatorId == null) {
            throw ProductDomainException.reportError("操作人ID不能为空");
        }
        if (action == null || action.isBlank()) {
            throw ProductDomainException.reportError("动作类型不能为空");
        }
        return new ReportHandleHistory(id, reportId, operatorId, action, remark, LocalDateTime.now());
    }

    public static ReportHandleHistory reconstitute(
            String id, String reportId, String operatorId, String action, String remark, LocalDateTime createTime) {
        return new ReportHandleHistory(id, reportId, operatorId, action, remark, createTime);
    }
}
