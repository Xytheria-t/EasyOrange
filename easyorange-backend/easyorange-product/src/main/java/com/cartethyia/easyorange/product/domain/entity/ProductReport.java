package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductReport {

    private final String id;
    private final String productId;
    private final String reporterId;
    private final String reason;
    private final Integer reasonType;
    private final ProductReportStatus status;
    private final String remark;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    private ProductReport(String id, String productId, String reporterId, String reason,
                          Integer reasonType, ProductReportStatus status,
                          String remark, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.productId = productId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.reasonType = reasonType;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public static ProductReport create(String productId, String reporterId, String reason, Integer reasonType) {
        if (productId == null) {
            throw new ReportDomainException("资产ID不能为空");
        }
        if (reporterId == null) {
            throw new ReportDomainException("举报人ID不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new ReportDomainException("举报原因不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        return new ProductReport(null, productId, reporterId, reason, reasonType,
                ProductReportStatus.PENDING, null, now, now);
    }

    public static ProductReport reconstitute(String id, String productId, String reporterId,
                                              String reason, ProductReportStatus status,
                                              String remark, LocalDateTime createTime,
                                              LocalDateTime updateTime, Integer reasonType) {
        return new ProductReport(id, productId, reporterId, reason, reasonType,
                status, remark, createTime, updateTime);
    }

    public ProductReport approve(String remark) {
        if (!isPending()) {
            throw new ReportDomainException("只有待处理的举报才能被批准");
        }
        return new ProductReport(id, productId, reporterId, reason, reasonType,
                ProductReportStatus.RESOLVED, remark, createTime, LocalDateTime.now());
    }

    public ProductReport reject(String remark) {
        if (!isPending()) {
            throw new ReportDomainException("只有待处理的举报才能被驳回");
        }
        return new ProductReport(id, productId, reporterId, reason, reasonType,
                ProductReportStatus.DISMISSED, remark, createTime, LocalDateTime.now());
    }

    public boolean isPending() {
        return ProductReportStatus.PENDING.equals(this.status);
    }

    public Integer statusCode() {
        return status != null ? status.getCode() : null;
    }

    public ProductReport assignId(String id) {
        if (this.id != null) {
            return this;
        }
        return new ProductReport(id, productId, reporterId, reason, reasonType,
                status, remark, createTime, updateTime);
    }

    public static class ReportDomainException extends BaseBusinessException {
        public ReportDomainException(String message) {
            super(message);
        }
        @Override
        protected String defaultCode() {
            return ProductResultCode.REPORT_ERROR.getCode();
        }
    }
}
