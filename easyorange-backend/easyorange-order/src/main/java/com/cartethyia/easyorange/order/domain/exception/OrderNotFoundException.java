package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.order.enums.OrderResultCode;

public class OrderNotFoundException extends OrderDomainException {

    public OrderNotFoundException(Long orderId) {
        super(OrderResultCode.ORDER_NOT_FOUND, "订单不存在: id=" + orderId);
    }

    public OrderNotFoundException(String orderNo) {
        super(OrderResultCode.ORDER_NOT_FOUND, "订单不存在: orderNo=" + orderNo);
    }
}
