package com.cartethyia.easyorange.order.domain.saga;

/**
 * 分布式锁获取异常
 */
public class SagaLockAcquisitionException extends SagaException {

    public SagaLockAcquisitionException(String message) {
        super(message);
    }

    public SagaLockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}