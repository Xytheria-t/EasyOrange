package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductMarkedSoldEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Product";

    private Long productId;

    public ProductMarkedSoldEvent(Long productId) {
        super(AGGREGATE_TYPE);
        this.productId = productId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getProductId() {
        return productId;
    }

    @Override
    public String eventType() {
        return "ProductMarkedSold";
    }

    public static class Builder {
        private Long productId;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public ProductMarkedSoldEvent build() {
            return new ProductMarkedSoldEvent(productId);
        }
    }
}