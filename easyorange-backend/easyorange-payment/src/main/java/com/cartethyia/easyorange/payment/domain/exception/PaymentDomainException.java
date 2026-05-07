package com.cartethyia.easyorange.payment.domain.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;

public class PaymentDomainException extends BaseBusinessException {

    protected PaymentDomainException(String message) {
        super(message);
    }

    protected PaymentDomainException(IResultCode resultCode) {
        super(resultCode);
    }

    protected PaymentDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    protected PaymentDomainException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    protected PaymentDomainException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

    @Override
    protected String defaultCode() {
        return PaymentResultCode.PAYMENT_FAILED.getCode();
    }

    public static PaymentDomainException of(String message) {
        return new PaymentDomainException(message);
    }

    public static PaymentDomainException of(IResultCode resultCode) {
        return new PaymentDomainException(resultCode);
    }

    public static PaymentDomainException of(String message, Throwable cause) {
        return new PaymentDomainException(message, cause);
    }

    public static PaymentDomainException of(IResultCode resultCode, String message) {
        return new PaymentDomainException(resultCode, message);
    }
}
