package com.cartethyia.easyorange.order.domain.saga;

/**
 * Saga 序列化异常
 */
public class SagaSerializationException extends SagaException {

    public SagaSerializationException(String message) {
        super(message);
    }

    public SagaSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SagaSerializationException(String sagaId, String message, Throwable cause) {
        super(sagaId, SagaState.PENDING, message, cause);
    }
}