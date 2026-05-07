package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;

public class OrderDomainException extends BusinessException {

    public OrderDomainException(String message) {
        super(message);
    }

    public OrderDomainException(OrderResultCode resultCode) {
        super(resultCode);
    }

    public OrderDomainException(OrderResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrderDomainException(OrderResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }
}
