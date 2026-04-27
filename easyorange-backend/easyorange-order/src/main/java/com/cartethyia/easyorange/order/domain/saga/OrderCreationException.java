package com.cartethyia.easyorange.order.domain.saga;

/**
 * 订单创建异常
 */
public class OrderCreationException extends RuntimeException {
    
    public OrderCreationException(String message) {
        super(message);
    }
    
    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
