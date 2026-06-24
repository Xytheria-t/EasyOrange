package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.math.BigDecimal;

/**
 * 出价被还价事件 — AI 对买家出价进行还价时发布。
 */
public class OfferCounteredEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long sellerId;
    private final Long buyerId;
    private final BigDecimal offerPrice;
    private final BigDecimal counterPrice;

    public OfferCounteredEvent(Long productId, Long sellerId, Long buyerId,
                               BigDecimal offerPrice, BigDecimal counterPrice) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.offerPrice = offerPrice;
        this.counterPrice = counterPrice;
    }

    public Long getProductId() { return productId; }
    public Long getSellerId() { return sellerId; }
    public Long getBuyerId() { return buyerId; }
    public BigDecimal getOfferPrice() { return offerPrice; }
    public BigDecimal getCounterPrice() { return counterPrice; }
}
