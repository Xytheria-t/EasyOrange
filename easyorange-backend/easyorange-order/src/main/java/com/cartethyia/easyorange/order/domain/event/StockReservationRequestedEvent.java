package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class StockReservationRequestedEvent extends BaseDomainEvent {

    private final String orderId;
    private final String productId;
    private final int quantity;

    public StockReservationRequestedEvent(String orderId, String productId, int quantity) {
        super();
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
