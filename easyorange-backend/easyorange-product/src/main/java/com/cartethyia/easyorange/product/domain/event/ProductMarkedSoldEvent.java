package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record ProductMarkedSoldEvent(String productId, String sellerId) implements DomainEvent {
    @Override
    public String aggregateId() {
        return productId;
    }
}
