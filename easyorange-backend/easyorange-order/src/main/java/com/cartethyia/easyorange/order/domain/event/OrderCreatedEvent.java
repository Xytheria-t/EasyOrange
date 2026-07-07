package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String buyerId,
        String sellerId,
        List<OrderItemPayload> items,
        BigDecimal totalAmount
) implements DomainEvent {

    public OrderCreatedEvent {
        items = List.copyOf(items);
    }

    public record OrderItemPayload(String productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
