package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BizRequire {

    private BizRequire() {}

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw BusinessException.of(message);
        }
    }

    public static void require(boolean condition, IResultCode resultCode) {
        if (!condition) {
            throw BusinessException.of(resultCode);
        }
    }

    public static void require(boolean condition, Runnable exceptionSupplier) {
        if (!condition) {
            throw BusinessException.of("业务规则不满足");
        }
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw BusinessException.of(message);
        }
        return obj;
    }

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

    public static <T> void notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw BusinessException.of(message);
        }
    }

    public static <T> void noNullElements(List<T> list, String message) {
        if (list == null || list.contains(null)) {
            throw BusinessException.of(message);
        }
    }

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

    public static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw BusinessException.of(message);
        }
    }

    public static <T> void eq(T expected, T actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw BusinessException.of(message);
        }
    }

    public static <T> void eq(T expected, T actual, IResultCode resultCode) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw BusinessException.of(resultCode);
        }
    }

    public static <T> void ne(T notExpected, T actual, String message) {
        if (notExpected == null && actual == null) {
            throw BusinessException.of(message);
        }
        if (notExpected != null && notExpected.equals(actual)) {
            throw BusinessException.of(message);
        }
    }

    public static <T> void ne(T notExpected, T actual, IResultCode resultCode) {
        if (notExpected == null && actual == null) {
            throw BusinessException.of(resultCode);
        }
        if (notExpected != null && notExpected.equals(actual)) {
            throw BusinessException.of(resultCode);
        }
    }

    public static void between(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw BusinessException.of(message);
        }
    }

    public static void positive(Long value, String message) {
        if (value == null || value <= 0) {
            throw BusinessException.of(message);
        }
    }

    public static void positive(int value, String message) {
        if (value <= 0) {
            throw BusinessException.of(message);
        }
    }

    public static void nonNegative(Integer value, String message) {
        if (value == null || value < 0) {
            throw BusinessException.of(message);
        }
    }

    public static void nonNegative(int value, String message) {
        if (value < 0) {
            throw BusinessException.of(message);
        }
    }
}