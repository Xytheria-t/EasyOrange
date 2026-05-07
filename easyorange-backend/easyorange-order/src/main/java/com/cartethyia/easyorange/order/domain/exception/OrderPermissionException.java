package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;

public class OrderPermissionException extends OrderDomainException {

    public OrderPermissionException(Long orderId, Long userId, String requiredRole) {
        super(OrderResultCode.ORDER_NOT_OWNER, 
            String.format("无权限操作订单: orderId=%d, userId=%d, 需要角色=%s", 
                orderId, userId, requiredRole));
    }

    public OrderPermissionException(Long orderId, Long userId) {
        super(OrderResultCode.ORDER_NOT_OWNER, 
            String.format("非订单所有者: orderId=%d, userId=%d", orderId, userId));
    }
}
