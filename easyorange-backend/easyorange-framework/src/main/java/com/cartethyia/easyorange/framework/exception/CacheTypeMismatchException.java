package com.cartethyia.easyorange.framework.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import lombok.Getter;

/**
 * 缓存类型不匹配异常（框架内部编程错误信号）。
 * <p>
 * 故意不继承 {@link BaseBusinessException}，因为这是编程错误而非业务规则违反。
 * 如果到达生产环境，将被全局异常处理器兜底为 500。
 * </p>
 */
@Getter
public class CacheTypeMismatchException extends RuntimeException {

    private final Class<?> expectedType;
    private final Class<?> actualType;
    private final String key;

    public CacheTypeMismatchException(String key, Class<?> expectedType, Class<?> actualType) {
        super(String.format("缓存类型不匹配 - Key: %s, 期望类型: %s, 实际类型: %s",
                key, expectedType.getName(), actualType.getName()));
        this.key = key;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

}
