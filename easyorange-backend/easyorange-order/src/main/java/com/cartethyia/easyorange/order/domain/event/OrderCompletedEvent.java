package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class OrderCompletedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;
    private final Long productId;

    public OrderCompletedEvent(Long orderId, Long productId) {
        super(AGGREGATE_TYPE);
        this.orderId = orderId;
        this.productId = productId;
    }
}
