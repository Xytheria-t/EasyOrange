package com.cartethyia.easyorange.payment.domain.exception;

import com.cartethyia.easyorange.payment.enums.PaymentResultCode;

public class CallbackSignInvalidException extends PaymentDomainException {

    public CallbackSignInvalidException() {
        super(PaymentResultCode.CALLBACK_SIGN_INVALID);
    }

    public CallbackSignInvalidException(String message) {
        super(PaymentResultCode.CALLBACK_SIGN_INVALID, message);
    }

    public static CallbackSignInvalidException of() {
        return new CallbackSignInvalidException();
    }

    public static CallbackSignInvalidException of(String message) {
        return new CallbackSignInvalidException(message);
    }
}
