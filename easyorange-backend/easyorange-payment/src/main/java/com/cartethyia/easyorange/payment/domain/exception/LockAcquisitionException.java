package com.cartethyia.easyorange.payment.domain.exception;

/**
 * 分布式锁获取失败异常 — 由 {@code LockPort} 实现（Redisson 适配器）抛出。
 * <p>
 * 调用方（支付用例 {@code PaymentCommandHandler}）据此记录并发冲突指标
 * 并映射为业务响应；异常保持 RuntimeException，由全局异常处理器兜底为 500。
 */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String message) {
        super(message);
    }
}
