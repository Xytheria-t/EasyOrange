package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportHandleHistory {

    private String id;
    private final String reportId;
    private final String operatorId;
    private final String action;
    private final String remark;
    private LocalDateTime createTime;

    public ReportHandleHistory(String reportId, String operatorId, String action, String remark) {
        this.reportId = reportId;
        this.operatorId = operatorId;
        this.action = action;
        this.remark = remark;
        this.createTime = LocalDateTime.now();
    }

    public static ReportHandleHistory create(String reportId, String operatorId, String action, String remark) {
        if (reportId == null) {
            throw new HistoryDomainException("举报ID不能为空");
        }
        if (operatorId == null) {
            throw new HistoryDomainException("操作人ID不能为空");
        }
        if (action == null || action.isBlank()) {
            throw new HistoryDomainException("动作类型不能为空");
        }
        return new ReportHandleHistory(reportId, operatorId, action, remark);
    }

    public static ReportHandleHistory reconstitute(String id, String reportId, String operatorId,
                                                     String action, String remark,
                                                     LocalDateTime createTime) {
        ReportHandleHistory history = new ReportHandleHistory(reportId, operatorId, action, remark);
        history.id = id;
        history.createTime = createTime;
        return history;
    }

    public void assignId(String id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public static class HistoryDomainException extends BaseBusinessException {
        public HistoryDomainException(String message) {
            super(message);
        }
        @Override
        protected String defaultCode() {
            return ProductResultCode.REPORT_ERROR.getCode();
        }
    }
}
