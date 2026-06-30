package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class StockRestoredEvent extends BaseDomainEvent {

    private final String productId;
    private final int quantity;

    public StockRestoredEvent(String productId) {
        super();
        this.productId = productId;
        this.quantity = 1;
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }

}
