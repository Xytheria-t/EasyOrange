package com.cartethyia.easyorange.payment.domain.exception;

import com.cartethyia.easyorange.payment.enums.PaymentResultCode;

public class RefundNotAllowedException extends PaymentDomainException {

    public RefundNotAllowedException() {
        super(PaymentResultCode.REFUND_NOT_ALLOWED);
    }

    public RefundNotAllowedException(String message) {
        super(PaymentResultCode.REFUND_NOT_ALLOWED, message);
    }

    public static RefundNotAllowedException of() {
        return new RefundNotAllowedException();
    }

    public static RefundNotAllowedException of(String message) {
        return new RefundNotAllowedException(message);
    }
}
