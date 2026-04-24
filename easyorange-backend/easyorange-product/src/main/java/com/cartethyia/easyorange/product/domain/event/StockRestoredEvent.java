package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public class StockRestoredEvent extends BaseDomainEvent {

    private Long productId;
    private Integer quantity = 1;

    public StockRestoredEvent(Long productId) {
        super(StockRestoredEvent.class);
        this.productId = productId;
        this.quantity = 1;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    @Override
    public String eventType() {
        return "StockRestored";
    }

    public static class Builder {
        private Long productId;
        private Integer quantity = 1;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public StockRestoredEvent build() {
            StockRestoredEvent event = new StockRestoredEvent(productId);
            if (quantity != null) {
                event.quantity = quantity;
            }
            return event;
        }
    }
}