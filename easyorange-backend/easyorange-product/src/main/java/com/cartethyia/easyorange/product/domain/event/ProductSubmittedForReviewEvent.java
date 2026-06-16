package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.time.LocalDateTime;

public class ProductSubmittedForReviewEvent extends BaseDomainEvent {

    private final Long productId;
    private final Long sellerId;
    private final Integer beforeStatus;
    private final Integer afterStatus;

    public ProductSubmittedForReviewEvent(Long productId, Long sellerId,
                                          Integer beforeStatus, Integer afterStatus) {
        super();
        this.productId = productId;
        this.sellerId = sellerId;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Integer getBeforeStatus() {
        return beforeStatus;
    }

    public Integer getAfterStatus() {
        return afterStatus;
    }
}
