package com.cartethyia.easyorange.product.domain.event;

public record StockDecreasedEvent(String productId, int quantity) implements ProductEvent {

    public static StockDecreasedEvent of(String productId, int quantity) {
        return new StockDecreasedEvent(productId, quantity);
    }
}
