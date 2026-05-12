package com.cartethyia.easyorange.product.domain.event;

import java.time.LocalDateTime;

public record ProductAuditedEvent(
    Long productId,
    String productName,
    Long sellerId,
    Integer action,
    String reason,
    LocalDateTime auditTime
) {}
