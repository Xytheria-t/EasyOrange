package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCancelledEvent extends BaseDomainEvent {

    private final String orderId;
    private final List<String> productIds;
    private final String reason;

    public OrderCancelledEvent(String orderId, List<String> productIds, String reason) {
        super();
        this.orderId = orderId;
        this.productIds = productIds;
        this.reason = reason;
    }
}
