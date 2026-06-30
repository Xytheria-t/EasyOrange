package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductAuditedEvent extends BaseDomainEvent {

    private final String productId;
    private final String productName;
    private final String sellerId;
    private final Integer action;
    private final String reason;
    private final LocalDateTime auditTime;

    public ProductAuditedEvent(String productId, String productName, String sellerId,
                               Integer action, String reason, LocalDateTime auditTime) {
        super();
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.action = action;
        this.reason = reason;
        this.auditTime = auditTime;
    }

    // Record-style accessors for backward compatibility
    public String productId() { return productId; }
    public String productName() { return productName; }
    public String sellerId() { return sellerId; }
    public Integer action() { return action; }
    public String reason() { return reason; }
    public LocalDateTime auditTime() { return auditTime; }
}
