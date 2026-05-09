package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class StockReservationRequestedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long productId;
    private final int quantity;

    public StockReservationRequestedEvent(Long orderId, Long productId, int quantity) {
        super(StockReservationRequestedEvent.class);
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public String eventType() {
        return "StockReservationRequested";
    }
}
