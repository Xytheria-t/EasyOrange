package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductDeletedEvent extends BaseDomainEvent {

    private final String productId;
    private final String userId;

    public ProductDeletedEvent(String productId, String userId) {
        super();
        this.productId = productId;
        this.userId = userId;
    }

    public String getProductId() { return productId; }
    public String getUserId() { return userId; }

}
