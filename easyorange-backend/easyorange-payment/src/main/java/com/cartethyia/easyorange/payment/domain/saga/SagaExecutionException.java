package com.cartethyia.easyorange.payment.domain.saga;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import lombok.Getter;

@Getter
public class SagaExecutionException extends BaseBusinessException {
    private final String failedStep;

    public SagaExecutionException(String failedStep, String message) {
        super(PaymentResultCode.SAGA_EXECUTION_FAILED, message);
        this.failedStep = failedStep;
    }

    public SagaExecutionException(String failedStep, String message, Throwable cause) {
        super(PaymentResultCode.SAGA_EXECUTION_FAILED, message, cause);
        this.failedStep = failedStep;
    }
}
