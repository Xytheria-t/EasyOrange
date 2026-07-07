package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record StockDecreasedEvent(String productId, int quantity) implements DomainEvent {

    public static StockDecreasedEvent of(String productId) {
        return new StockDecreasedEvent(productId, 1);
    }
}
