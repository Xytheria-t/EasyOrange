package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCompletedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final List<Long> productIds;

    public OrderCompletedEvent(Long orderId, List<Long> productIds) {
        super();
        this.orderId = orderId;
        this.productIds = productIds;
    }
}
