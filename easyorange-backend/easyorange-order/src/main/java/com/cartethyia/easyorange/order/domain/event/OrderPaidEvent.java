package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 订单已付款事件
 */
@Getter
public class OrderPaidEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;
    private final Integer paymentStatus;

    public OrderPaidEvent(Long orderId, Integer paymentStatus) {
        super(AGGREGATE_TYPE);
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String eventType() {
        return "OrderPaid";
    }
}
