package com.cartethyia.easyorange.framework.exception;

import lombok.Getter;

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
