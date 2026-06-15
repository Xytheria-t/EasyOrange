package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class BizRequire {

    private BizRequire() {}

    // --- notNull ---

    public static <T> void notNull(T obj, String message) {
        if (obj == null) {
            throw BusinessException.of(message);
        }
    }

    public static <T> void notNull(T obj, IResultCode resultCode) {
        if (obj == null) {
            throw BusinessException.of(resultCode);
        }
    }

    // --- notBlank ---

    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) {
            throw BusinessException.of(message);
        }
    }

    public static void notBlank(String str, IResultCode resultCode) {
        if (str == null || str.isBlank()) {
            throw BusinessException.of(resultCode);
        }
    }

    // --- notEmpty ---

    public static <T> void notEmpty(Collection<T> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw BusinessException.of(message);
        }
    }

    public static <T> void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw BusinessException.of(message);
        }
    }

    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw BusinessException.of(message);
        }
    }

    // --- requireTrue ---

    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw BusinessException.of(message);
        }
    }

    public static void requireTrue(boolean condition, IResultCode resultCode) {
        if (!condition) {
            throw BusinessException.of(resultCode);
        }
    }
}
