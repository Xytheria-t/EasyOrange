package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportProcessedEvent extends BaseDomainEvent {

    private final Long reportId;
    private final Long reporterId;
    private final Long productId;
    private final boolean approved;
    private final String remark;
    private final LocalDateTime processedTime;

    public ReportProcessedEvent(Long reportId, Long reporterId, Long productId,
                                boolean approved, String remark, LocalDateTime processedTime) {
        super(ReportProcessedEvent.class);
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.productId = productId;
        this.approved = approved;
        this.remark = remark;
        this.processedTime = processedTime;
    }

    @Override
    public String eventType() {
        return "ReportProcessed";
    }

    // Record-style accessors for backward compatibility
    public Long reportId() { return reportId; }
    public Long reporterId() { return reporterId; }
    public Long productId() { return productId; }
    public boolean approved() { return approved; }
    public String remark() { return remark; }
    public LocalDateTime processedTime() { return processedTime; }
}