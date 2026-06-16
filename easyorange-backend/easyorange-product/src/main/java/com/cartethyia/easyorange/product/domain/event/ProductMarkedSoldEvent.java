package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductMarkedSoldEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long sellerId;

    public ProductMarkedSoldEvent(Long productId, Long sellerId) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
    }

    public Long getProductId() { return productId; }

    public Long getSellerId() { return sellerId; }

}
