package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.math.BigDecimal;

public class PriceAdjustedEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long sellerId;
    private final BigDecimal newPrice;
    private final Integer priceLevel;

    public PriceAdjustedEvent(Long productId, Long sellerId, BigDecimal newPrice, Integer priceLevel) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
        this.newPrice = newPrice;
        this.priceLevel = priceLevel;
    }

    public Long getProductId() { return productId; }
    public Long getSellerId() { return sellerId; }
    public BigDecimal getNewPrice() { return newPrice; }
    public Integer getPriceLevel() { return priceLevel; }

    // Record-style accessors for backward compatibility with domain tests
    public Long productId() { return productId; }
    public Long sellerId() { return sellerId; }
    public BigDecimal newPrice() { return newPrice; }
    public Integer priceLevel() { return priceLevel; }
}
