package com.cartethyia.easyorange.payment.domain.exception;

import com.cartethyia.easyorange.payment.enums.PaymentResultCode;

public class PaymentNotFoundException extends PaymentDomainException {

    public PaymentNotFoundException() {
        super(PaymentResultCode.PAYMENT_NOT_FOUND);
    }

    public PaymentNotFoundException(String message) {
        super(PaymentResultCode.PAYMENT_NOT_FOUND, message);
    }

    public static PaymentNotFoundException of() {
        return new PaymentNotFoundException();
    }

    public static PaymentNotFoundException of(String message) {
        return new PaymentNotFoundException(message);
    }
}
