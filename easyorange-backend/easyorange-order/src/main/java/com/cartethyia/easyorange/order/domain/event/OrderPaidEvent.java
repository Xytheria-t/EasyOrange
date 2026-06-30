package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 订单已付款事件
 */
@Getter
public class OrderPaidEvent extends BaseDomainEvent {

    private final String orderId;
    private final Integer paymentStatus;

    public OrderPaidEvent(String orderId, Integer paymentStatus) {
        super();
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
    }
}
