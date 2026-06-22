package com.cartethyia.easyorange.order.domain.saga;

/**
 * Saga 补偿操作异常
 */
public class SagaCompensationException extends SagaException {

    public SagaCompensationException(String message) {
        super(message);
    }

    public SagaCompensationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SagaCompensationException(String sagaId, SagaState state, String message, Throwable cause) {
        super(sagaId, state, message, cause);
    }
}