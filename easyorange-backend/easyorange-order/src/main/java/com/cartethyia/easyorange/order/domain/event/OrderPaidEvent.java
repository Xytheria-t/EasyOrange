package com.cartethyia.easyorange.order.domain.event;

/**
 * 订单已付款事件
 */
public record OrderPaidEvent(String eventId, String orderId, String buyerId, String paymentStatus)
        implements OrderEvent {

    @Override
    public String orderId() {
        return orderId;
    }
}
