package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 订单已发货事件
 */
@Getter
public class OrderShippedEvent extends BaseDomainEvent {

    private final Long orderId;

    public OrderShippedEvent(Long orderId) {
        super(OrderShippedEvent.class);
        this.orderId = orderId;
    }

    @Override
    public String eventType() {
        return "OrderShipped";
    }
}
