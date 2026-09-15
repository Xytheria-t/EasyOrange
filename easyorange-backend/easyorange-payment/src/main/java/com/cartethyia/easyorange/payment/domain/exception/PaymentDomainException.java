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

    /**
     * 支付单不存在（B4001）— 消息带定位键（如 {@code orderId=xxx}），供日志与排障定位。
     * <p>
     * 越权防护场景请直接用裸码 {@code of(PAYMENT_NOT_FOUND)}：不带定位键才能让
     * 「记录不存在」与「存在但非本人」的响应完全一致，避免泄露支付单存在性。
     */
    public static PaymentDomainException notFound(String locator) {
        return new PaymentDomainException(PaymentResultCode.PAYMENT_NOT_FOUND, "支付记录不存在: " + locator);
    }
}
