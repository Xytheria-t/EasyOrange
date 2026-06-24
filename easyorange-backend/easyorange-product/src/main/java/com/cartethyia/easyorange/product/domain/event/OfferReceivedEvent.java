package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.math.BigDecimal;

/**
 * 收到买家出价事件 — 买家对 AI 托管商品发起出价时发布。
 */
public class OfferReceivedEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long sellerId;
    private final Long buyerId;
    private final BigDecimal offerPrice;

    public OfferReceivedEvent(Long productId, Long sellerId, Long buyerId, BigDecimal offerPrice) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.offerPrice = offerPrice;
    }

    public Long getProductId() { return productId; }
    public Long getSellerId() { return sellerId; }
    public Long getBuyerId() { return buyerId; }
    public BigDecimal getOfferPrice() { return offerPrice; }
}
