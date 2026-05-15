package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductReport {

    private Long id;
    private final Long productId;
    private final Long reporterId;
    private final String reason;
    private Integer reasonType;
    private ProductReportStatus status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private ProductReport(Long productId, Long reporterId, String reason, Integer reasonType) {
        this.productId = productId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.reasonType = reasonType;
        this.status = ProductReportStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public static ProductReport create(Long productId, Long reporterId, String reason, Integer reasonType) {
        if (productId == null) {
            throw new ReportDomainException("商品ID不能为空");
        }
        if (reporterId == null) {
            throw new ReportDomainException("举报人ID不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new ReportDomainException("举报原因不能为空");
        }
        return new ProductReport(productId, reporterId, reason, reasonType);
    }

    public static ProductReport reconstitute(Long id, Long productId, Long reporterId,
                                              String reason, ProductReportStatus status,
                                              String remark, LocalDateTime createTime,
                                              LocalDateTime updateTime, Integer reasonType) {
        ProductReport report = new ProductReport(productId, reporterId, reason, reasonType);
        report.id = id;
        report.status = status;
        report.remark = remark;
        report.createTime = createTime;
        report.updateTime = updateTime;
        return report;
    }

    public void approve(String remark) {
        if (!isPending()) {
            throw new ReportDomainException("只有待处理的举报才能被批准");
        }
        this.status = ProductReportStatus.RESOLVED;
        this.remark = remark;
        this.updateTime = LocalDateTime.now();
    }

    public void reject(String remark) {
        if (!isPending()) {
            throw new ReportDomainException("只有待处理的举报才能被驳回");
        }
        this.status = ProductReportStatus.DISMISSED;
        this.remark = remark;
        this.updateTime = LocalDateTime.now();
    }

    public boolean isPending() {
        return ProductReportStatus.PENDING.equals(this.status);
    }

    public Integer statusCode() {
        return status != null ? status.getCode() : null;
    }

    public void assignId(Long id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public static class ReportDomainException extends BaseBusinessException {
        public ReportDomainException(String message) {
            super(message);
        }
    }
}
