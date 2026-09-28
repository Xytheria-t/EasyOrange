package com.cartethyia.easyorange.order.domain.event;

/**
 * 订单已付款事件 — 支付状态由消费方回查订单获得，事件不重复携带（聚合根上已有枚举字段）。
 */
public record OrderPaidEvent(String eventId, String orderId, String buyerId) implements OrderEvent {}
