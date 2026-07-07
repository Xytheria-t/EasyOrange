package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.util.List;

public record OrderCancelledEvent(String orderId, List<String> productIds, String reason) implements DomainEvent {

    public OrderCancelledEvent {
        productIds = List.copyOf(productIds);
    }
}
