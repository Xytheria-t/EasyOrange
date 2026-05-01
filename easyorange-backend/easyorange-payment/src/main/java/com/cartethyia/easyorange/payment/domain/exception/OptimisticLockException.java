package com.cartethyia.easyorange.payment.domain.exception;

public class OptimisticLockException extends PaymentDomainException {
    
    public OptimisticLockException(String message) {
        super(message);
    }
    
    public static OptimisticLockException of(String message) {
        return new OptimisticLockException(message);
    }
    
    public static OptimisticLockException concurrentUpdate(Long paymentId) {
        return new OptimisticLockException(
            "并发更新冲突，支付记录已被其他事务修改: paymentId=" + paymentId
        );
    }
}
