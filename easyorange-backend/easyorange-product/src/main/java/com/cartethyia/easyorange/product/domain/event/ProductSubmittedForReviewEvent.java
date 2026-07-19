package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.time.LocalDateTime;

public record ProductSubmittedForReviewEvent(String productId, String sellerId,
                                             Integer beforeStatus, Integer afterStatus) implements DomainEvent {
    @Override
    public String aggregateId() {
        return productId;
    }
}
