package com.cartethyia.easyorange.product.domain.event;

import java.time.LocalDateTime;

public record ProductAuditedEvent(String productId, String productName, String sellerId,
                                  Integer action, String reason, LocalDateTime auditTime) implements ProductEvent {
}
