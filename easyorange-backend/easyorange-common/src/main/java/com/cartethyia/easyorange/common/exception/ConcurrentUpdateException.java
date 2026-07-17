package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.ResultCode;

/**
 * 并发更新冲突异常。
 * <p>
 * 当 MyBatis-Plus {@code updateById} 返回 0 行时抛出（乐观锁版本不匹配或记录已被删除）。
 * 各模块 Repository 实现按需直接 throw，带领域上下文消息。
 * </p>
 */
public class ConcurrentUpdateException extends BaseBusinessException {

    public ConcurrentUpdateException(String message) {
        super(ResultCode.CONCURRENT_UPDATE, message);
    }
}
