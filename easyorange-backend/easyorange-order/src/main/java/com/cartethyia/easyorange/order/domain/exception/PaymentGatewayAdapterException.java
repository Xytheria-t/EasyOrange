package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * 支付网关异常 — 上游（支付网关）不可用，D 前缀 502 语义（见 {@link ResultCode#UPSTREAM_ERROR}）。
 */
public class PaymentGatewayAdapterException extends BaseBusinessException {

    public PaymentGatewayAdapterException(String message) {
        super(message);
    }

    public PaymentGatewayAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected String defaultCode() {
        return ResultCode.UPSTREAM_ERROR.getCode();
    }
}
