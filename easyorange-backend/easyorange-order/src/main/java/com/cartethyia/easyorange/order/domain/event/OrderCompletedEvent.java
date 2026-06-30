package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCompletedEvent extends BaseDomainEvent {

    private final String orderId;
    private final List<String> productIds;

    public OrderCompletedEvent(String orderId, List<String> productIds) {
        super();
        this.orderId = orderId;
        this.productIds = productIds;
    }
}
