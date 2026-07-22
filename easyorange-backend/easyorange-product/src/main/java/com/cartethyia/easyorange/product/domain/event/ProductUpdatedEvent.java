package com.cartethyia.easyorange.product.domain.event;

public record ProductUpdatedEvent(ProductEvent.Data data) implements ProductEvent {

    @Override
    public String productId() {
        return data.productId();
    }
}
