package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record ProductDeletedEvent(String productId, String userId) implements DomainEvent {
    @Override
    public String aggregateId() {
        return productId;
    }
}
