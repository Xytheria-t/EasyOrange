package com.cartethyia.easyorange.order.domain.event;

import java.util.List;

/**
 * 订单退款事件 — 携带资产明细（ID + 数量），消费端据此按实际数量恢复库存并触发退款。
 */
public record OrderRefundedEvent(
        String eventId, String orderId, String buyerId, List<OrderItemRef> items, String reason) implements OrderEvent {

    public OrderRefundedEvent {
        items = items == null ? List.of() : List.copyOf(items);
    }

    @Override
    public String orderId() {
        return orderId;
    }
}
