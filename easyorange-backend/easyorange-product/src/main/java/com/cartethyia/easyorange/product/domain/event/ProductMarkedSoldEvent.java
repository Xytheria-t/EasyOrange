package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductMarkedSoldEvent extends BaseDomainEvent {

    private final Long productId;

    public ProductMarkedSoldEvent(Long productId) {
        super(ProductMarkedSoldEvent.class);
        this.productId = productId;
    }

    public Long getProductId() { return productId; }

    @Override
    public String eventType() {
        return "ProductMarkedSold";
    }
}
