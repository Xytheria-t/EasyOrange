package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * Saga 执行异常
 */
public class SagaException extends BaseBusinessException {

    private final String sagaId;
    private final SagaState state;

    public SagaException(String message) {
        super(message);
        this.sagaId = null;
        this.state = null;
    }

    public SagaException(String message, Throwable cause) {
        super(message, cause);
        this.sagaId = null;
        this.state = null;
    }

    public SagaException(String sagaId, SagaState state, String message) {
        super(message);
        this.sagaId = sagaId;
        this.state = state;
    }

    public SagaException(String sagaId, SagaState state, String message, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.state = state;
    }

    public String getSagaId() {
        return sagaId;
    }

    public SagaState getState() {
        return state;
    }
}