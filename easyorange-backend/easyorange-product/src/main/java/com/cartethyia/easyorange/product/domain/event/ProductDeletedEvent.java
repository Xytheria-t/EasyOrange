package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class ProductDeletedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Product";

    private Long productId;
    private Long userId;

    public ProductDeletedEvent(Long productId, Long userId) {
        super(AGGREGATE_TYPE);
        this.productId = productId;
        this.userId = userId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String eventType() {
        return "ProductDeleted";
    }

    public static class Builder {
        private Long productId;
        private Long userId;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ProductDeletedEvent build() {
            return new ProductDeletedEvent(productId, userId);
        }
    }
}