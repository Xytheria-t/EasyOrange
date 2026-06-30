package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductMarkedSoldEvent extends BaseDomainEvent {

    private final String productId;
    private final String sellerId;

    public ProductMarkedSoldEvent(String productId, String sellerId) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
    }

    public String getProductId() { return productId; }

    public String getSellerId() { return sellerId; }

}
