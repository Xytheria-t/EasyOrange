package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * 支付网关异常
 */
public class PaymentGatewayAdapterException extends BaseBusinessException {

    public PaymentGatewayAdapterException(String message) {
        super(message);
    }

    public PaymentGatewayAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
