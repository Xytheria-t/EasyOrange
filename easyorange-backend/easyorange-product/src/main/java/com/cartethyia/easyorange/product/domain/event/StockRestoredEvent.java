package com.cartethyia.easyorange.product.domain.event;

public record StockRestoredEvent(String productId, int quantity) implements ProductEvent {

    public static StockRestoredEvent of(String productId, int quantity) {
        return new StockRestoredEvent(productId, quantity);
    }
}
