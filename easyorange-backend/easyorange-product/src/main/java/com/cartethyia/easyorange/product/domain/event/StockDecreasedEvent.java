package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.idgen.UuidV7;

public record StockDecreasedEvent(String eventId, String productId, int quantity) implements ProductEvent {

    public static StockDecreasedEvent of(String productId, int quantity) {
        return new StockDecreasedEvent(UuidV7.generateId(), productId, quantity);
    }
}
