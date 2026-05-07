package com.cartethyia.easyorange.payment.domain.exception;

import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;

public class PaymentInvalidStatusException extends PaymentDomainException {

    public PaymentInvalidStatusException() {
        super(PaymentResultCode.PAYMENT_INVALID_STATUS);
    }

    public PaymentInvalidStatusException(String message) {
        super(PaymentResultCode.PAYMENT_INVALID_STATUS, message);
    }

    public static PaymentInvalidStatusException of() {
        return new PaymentInvalidStatusException();
    }

    public static PaymentInvalidStatusException of(String message) {
        return new PaymentInvalidStatusException(message);
    }
}
