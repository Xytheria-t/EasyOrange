package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;

public class OrderStatusException extends OrderDomainException {

    public OrderStatusException(String orderId, OrderStatus currentStatus, String expectedStatus) {
        super(OrderResultCode.ORDER_STATUS_ERROR, 
            String.format("订单状态异常: orderId=%s, 当前状态=%s, 期望状态=%s", 
                orderId, currentStatus.getDesc(), expectedStatus));
    }

    public OrderStatusException(String orderId, String operation, OrderStatus currentStatus) {
        super(OrderResultCode.ORDER_STATUS_ERROR, 
            String.format("订单状态不允许%s: orderId=%s, 当前状态=%s", 
                operation, orderId, currentStatus.getDesc()));
    }
}
