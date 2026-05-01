package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.order.enums.OrderResultCode;

public class OrderOperationException extends OrderDomainException {

    public OrderOperationException(Long orderId, String operation, String reason) {
        super(OrderResultCode.ORDER_CANNOT_CANCEL, 
            String.format("订单操作失败: orderId=%d, 操作=%s, 原因=%s", 
                orderId, operation, reason));
    }

    public OrderOperationException(String operation, String reason) {
        super(OrderResultCode.ORDER_CANNOT_CANCEL, 
            String.format("订单操作失败: 操作=%s, 原因=%s", operation, reason));
    }
}
