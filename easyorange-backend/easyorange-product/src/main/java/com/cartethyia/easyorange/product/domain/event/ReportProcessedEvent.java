package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportProcessedEvent extends BaseDomainEvent {

    private final String reportId;
    private final String reporterId;
    private final String productId;
    private final boolean approved;
    private final String remark;
    private final LocalDateTime processedTime;

    public ReportProcessedEvent(String reportId, String reporterId, String productId,
                                boolean approved, String remark, LocalDateTime processedTime) {
        super();
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.productId = productId;
        this.approved = approved;
        this.remark = remark;
        this.processedTime = processedTime;
    }

    // Record-style accessors for backward compatibility
    public String reportId() { return reportId; }
    public String reporterId() { return reporterId; }
    public String productId() { return productId; }
    public boolean approved() { return approved; }
    public String remark() { return remark; }
    public LocalDateTime processedTime() { return processedTime; }
}