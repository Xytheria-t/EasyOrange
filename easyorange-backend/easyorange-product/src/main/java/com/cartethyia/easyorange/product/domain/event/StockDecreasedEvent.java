package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class StockDecreasedEvent extends BaseDomainEvent {

    private final Long productId;
    private final int quantity;

    public StockDecreasedEvent(Long productId) {
        super(StockDecreasedEvent.class);
        this.productId = productId;
        this.quantity = 1;
    }

    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }

    @Override
    public String eventType() {
        return "StockDecreased";
    }
}
