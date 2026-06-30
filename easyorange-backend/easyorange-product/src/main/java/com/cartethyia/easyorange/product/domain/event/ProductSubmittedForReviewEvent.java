package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.time.LocalDateTime;

public class ProductSubmittedForReviewEvent extends BaseDomainEvent {

    private final String productId;
    private final String sellerId;
    private final Integer beforeStatus;
    private final Integer afterStatus;

    public ProductSubmittedForReviewEvent(String productId, String sellerId,
                                          Integer beforeStatus, Integer afterStatus) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
    }

    public String getProductId() {
        return productId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public Integer getBeforeStatus() {
        return beforeStatus;
    }

    public Integer getAfterStatus() {
        return afterStatus;
    }
}
