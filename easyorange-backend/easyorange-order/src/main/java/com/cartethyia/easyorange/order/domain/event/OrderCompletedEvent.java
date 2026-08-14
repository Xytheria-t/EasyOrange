package com.cartethyia.easyorange.order.domain.event;

import java.util.List;

/**
 * 订单完成事件 — 携带买卖双方 ID，下游（站内信通知、信用分重算）自包含消费，无需回查。
 */
public record OrderCompletedEvent(
        String eventId, String orderId, String buyerId, String sellerId, List<String> productIds)
        implements OrderEvent {

    public OrderCompletedEvent {
        productIds = List.copyOf(productIds);
    }

    @Override
    public String orderId() {
        return orderId;
    }
}
