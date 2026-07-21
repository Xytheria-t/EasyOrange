package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.util.List;

public record OrderCompletedEvent(String orderId, List<String> productIds) implements OrderEvent {

    public OrderCompletedEvent {
        productIds = List.copyOf(productIds);
    }

    @Override
    public String orderId() {
        return orderId;
    }
}
