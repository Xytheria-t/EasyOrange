package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.math.BigDecimal;

/**
 * 出价被接受事件 — AI 接受买家出价并创建订单时发布。
 */
public class OfferAcceptedEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long sellerId;
    private final Long buyerId;
    private final BigDecimal acceptedPrice;
    private final Long orderId;

    public OfferAcceptedEvent(Long productId, Long sellerId, Long buyerId,
                              BigDecimal acceptedPrice, Long orderId) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.acceptedPrice = acceptedPrice;
        this.orderId = orderId;
    }

    public Long getProductId() { return productId; }
    public Long getSellerId() { return sellerId; }
    public Long getBuyerId() { return buyerId; }
    public BigDecimal getAcceptedPrice() { return acceptedPrice; }
    public Long getOrderId() { return orderId; }
}
