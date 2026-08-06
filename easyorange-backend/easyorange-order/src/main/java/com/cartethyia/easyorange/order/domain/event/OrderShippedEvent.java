package com.cartethyia.easyorange.order.domain.event;

/**
 * 订单已发货事件
 */
public record OrderShippedEvent(String orderId) implements OrderEvent {

    @Override
    public String orderId() {
        return orderId;
    }
}
