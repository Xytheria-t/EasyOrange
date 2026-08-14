package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.idgen.UuidV7;

public record StockRestoredEvent(String eventId, String productId, int quantity) implements ProductEvent {

    public static StockRestoredEvent of(String productId, int quantity) {
        return new StockRestoredEvent(UuidV7.generateId(), productId, quantity);
    }
}
