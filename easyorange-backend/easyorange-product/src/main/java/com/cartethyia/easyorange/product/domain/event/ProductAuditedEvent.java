package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductAuditedEvent extends BaseDomainEvent {

    private final Long productId;
    private final String productName;
    private final Long sellerId;
    private final Integer action;
    private final String reason;
    private final LocalDateTime auditTime;

    public ProductAuditedEvent(Long productId, String productName, Long sellerId,
                               Integer action, String reason, LocalDateTime auditTime) {
        super(ProductAuditedEvent.class);
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.action = action;
        this.reason = reason;
        this.auditTime = auditTime;
    }

    @Override
    public String eventType() {
        return "ProductAudited";
    }

    // Record-style accessors for backward compatibility
    public Long productId() { return productId; }
    public String productName() { return productName; }
    public Long sellerId() { return sellerId; }
    public Integer action() { return action; }
    public String reason() { return reason; }
    public LocalDateTime auditTime() { return auditTime; }
}