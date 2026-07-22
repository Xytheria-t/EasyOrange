package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record StockReservationRequestedEvent(String orderId, String productId, int quantity) implements OrderEvent {

    @Override
    public String orderId() {
        return orderId;
    }

    @Override
    public String aggregateId() {
        return orderId;
    }
}
