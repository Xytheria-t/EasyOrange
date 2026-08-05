package com.cartethyia.easyorange.framework.web.idempotency;

/**
 * 幂等执行的操作封装。
 *
 * @param <T> 返回值类型
 */
@FunctionalInterface
public interface IdempotentOperation<T> {

    /**
     * 执行业务操作。
     *
     * @return 操作结果
     * @throws Exception 操作中抛出的异常
     */
    T execute() throws Exception;
}
