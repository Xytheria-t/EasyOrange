package com.cartethyia.easyorange.order.domain.saga;

public enum SagaState {
    PENDING,
    ORDER_CREATED,
    PAYMENT_CREATED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
