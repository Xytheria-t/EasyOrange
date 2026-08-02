package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * 订单创建异常
 */
public class OrderCreationException extends BaseBusinessException {
    
    public OrderCreationException(String message) {
        super(message);
    }
    
    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
