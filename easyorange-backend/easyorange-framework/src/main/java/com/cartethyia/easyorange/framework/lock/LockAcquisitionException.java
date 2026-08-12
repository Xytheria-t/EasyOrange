package com.cartethyia.easyorange.framework.lock;

/**
 * 分布式锁获取失败异常 — 由 {@link DistributedLockPort} 实现（Redisson 适配器）抛出。
 * <p>
 * 属锁基础设施的「传输+响应型」异常：order 用例在边界映射为自己的 {@code OrderCreationException}（保留 B0002→400）；
 * payment 用例复用本类型兜底（RuntimeException→500），仅 catch 后以统一文案重抛。以此保留各自的错误码与提示文案。
 */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String message) {
        super(message);
    }

    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
