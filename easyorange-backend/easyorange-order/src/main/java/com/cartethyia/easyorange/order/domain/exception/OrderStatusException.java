package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.order.enums.OrderStatus;

public class OrderStatusException extends OrderDomainException {

    public OrderStatusException(Long orderId, OrderStatus currentStatus, String expectedStatus) {
        super(OrderResultCode.ORDER_STATUS_ERROR, 
            String.format("订单状态异常: orderId=%d, 当前状态=%s, 期望状态=%s", 
                orderId, currentStatus.getDesc(), expectedStatus));
    }

    public OrderStatusException(Long orderId, String operation, OrderStatus currentStatus) {
        super(OrderResultCode.ORDER_STATUS_ERROR, 
            String.format("订单状态不允许%s: orderId=%d, 当前状态=%s", 
                operation, orderId, currentStatus.getDesc()));
    }
}
