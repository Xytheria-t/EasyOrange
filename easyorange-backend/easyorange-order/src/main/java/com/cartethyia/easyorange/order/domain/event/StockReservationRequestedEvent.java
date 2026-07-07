package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record StockReservationRequestedEvent(String orderId, String productId, int quantity) implements DomainEvent {
}
