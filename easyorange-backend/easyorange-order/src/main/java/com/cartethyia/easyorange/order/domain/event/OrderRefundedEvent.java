package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class OrderRefundedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long productId;
    private final String reason;

    public OrderRefundedEvent(Long orderId, Long productId, String reason) {
        super(OrderRefundedEvent.class);
        this.orderId = orderId;
        this.productId = productId;
        this.reason = reason;
    }

    @Override
    public String eventType() {
        return "OrderRefunded";
    }
}
