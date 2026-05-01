package com.cartethyia.easyorange.payment.domain.saga;

public class SagaExecutionException extends RuntimeException {
    private final String failedStep;

    public SagaExecutionException(String failedStep, String message) {
        super(message);
        this.failedStep = failedStep;
    }

    public SagaExecutionException(String failedStep, String message, Throwable cause) {
        super(message, cause);
        this.failedStep = failedStep;
    }

    public String getFailedStep() {
        return failedStep;
    }
}
