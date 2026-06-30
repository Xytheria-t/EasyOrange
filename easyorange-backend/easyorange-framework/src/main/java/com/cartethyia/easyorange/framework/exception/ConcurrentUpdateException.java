package com.cartethyia.easyorange.framework.exception;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * 并发更新冲突异常。
 * <p>
 * 当 MyBatis-Plus {@code updateById} 返回 0 行时抛出（乐观锁版本不匹配或记录已被删除）。
 * 由 {@link com.cartethyia.easyorange.framework.repository.BaseRepository BaseRepository.updateById()} 统一抛出，
 * 各模块 Repository 实现无需重复检查。
 * </p>
 */
public class ConcurrentUpdateException extends BaseBusinessException {

    public ConcurrentUpdateException(String message) {
        super(ResultCode.CONCURRENT_UPDATE, message);
    }
}
