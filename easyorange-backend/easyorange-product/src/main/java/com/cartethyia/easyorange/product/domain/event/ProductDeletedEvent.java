package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductDeletedEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long userId;

    public ProductDeletedEvent(Long productId, Long userId) {
        super(ProductDeletedEvent.class);
        this.productId = productId;
        this.userId = userId;
    }

    public Long getProductId() { return productId; }
    public Long getUserId() { return userId; }

    @Override
    public String eventType() {
        return "ProductDeleted";
    }
}
