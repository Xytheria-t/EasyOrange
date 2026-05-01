package com.cartethyia.easyorange.payment.domain.saga;

import lombok.Getter;

@Getter
public class SagaStepResult<T> {
    private final T data;
    private final boolean success;
    private final String errorMessage;
    private final Throwable cause;

    private SagaStepResult(T data, boolean success, String errorMessage, Throwable cause) {
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public static <T> SagaStepResult<T> success(T data) {
        return new SagaStepResult<>(data, true, null, null);
    }

    public static <T> SagaStepResult<T> failure(String errorMessage) {
        return new SagaStepResult<>(null, false, errorMessage, null);
    }

    public static <T> SagaStepResult<T> failure(String errorMessage, Throwable cause) {
        return new SagaStepResult<>(null, false, errorMessage, cause);
    }
}
