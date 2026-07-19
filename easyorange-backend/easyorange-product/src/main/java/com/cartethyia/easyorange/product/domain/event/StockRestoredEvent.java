package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record StockRestoredEvent(String productId, int quantity) implements DomainEvent {

    public static StockRestoredEvent of(String productId) {
        return new StockRestoredEvent(productId, 1);
    }

    @Override
    public String aggregateId() {
        return productId;
    }
}
