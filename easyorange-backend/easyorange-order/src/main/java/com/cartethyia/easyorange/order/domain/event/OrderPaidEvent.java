package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 订单已付款事件
 */
public record OrderPaidEvent(String orderId, Integer paymentStatus) implements OrderEvent {

    @Override
    public String orderId() {
        return orderId;
    }
}
