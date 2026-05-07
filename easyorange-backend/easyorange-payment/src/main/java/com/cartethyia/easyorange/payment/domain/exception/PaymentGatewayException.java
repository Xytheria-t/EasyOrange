package com.cartethyia.easyorange.payment.domain.exception;

import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;

public class PaymentGatewayException extends PaymentDomainException {

    public PaymentGatewayException() {
        super(PaymentResultCode.PAYMENT_GATEWAY_ERROR);
    }

    public PaymentGatewayException(String message) {
        super(PaymentResultCode.PAYMENT_GATEWAY_ERROR, message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(PaymentResultCode.PAYMENT_GATEWAY_ERROR, message, cause);
    }

    public static PaymentGatewayException of() {
        return new PaymentGatewayException();
    }

    public static PaymentGatewayException of(String message) {
        return new PaymentGatewayException(message);
    }

    public static PaymentGatewayException of(String message, Throwable cause) {
        return new PaymentGatewayException(message, cause);
    }
}
