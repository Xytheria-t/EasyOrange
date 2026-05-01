package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class BizRequire {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private BizRequire() {
        throw new IllegalStateException("Utility class");
    }

    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw BusinessException.of(message);
        }
    }

    public static void notNull(Object obj, IResultCode resultCode) {
        if (obj == null) {
            throw BusinessException.of(resultCode);
        }
    }

    public static void isNull(Object obj, String message) {
        if (obj != null) {
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

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw BusinessException.of(message);
        }
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw BusinessException.of(message);
        }
    }

    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw BusinessException.of(message);
        }
    }

    public static void positive(Number number, String message) {
        if (number == null || number.longValue() <= 0) {
            throw BusinessException.of(message);
        }
    }

    public static void nonNegative(Number number, String message) {
        if (number == null || number.longValue() < 0) {
            throw BusinessException.of(message);
        }
    }

    public static void between(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw BusinessException.of(message);
        }
    }

    public static void noNullElements(Collection<?> collection, String message) {
        if (collection != null) {
            for (Object obj : collection) {
                if (obj == null) {
                    throw BusinessException.of(message);
                }
            }
        }
    }

    public static void validEmail(String email, String message) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw BusinessException.of(message);
        }
    }

    public static void validPhone(String phone, String message) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw BusinessException.of(message);
        }
    }

    public static void fail(String message) {
        throw BusinessException.of(message);
    }

    public static void eq(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw BusinessException.of(message);
        }
    }

    public static void eq(Object expected, Object actual, IResultCode resultCode) {
        if (!Objects.equals(expected, actual)) {
            throw BusinessException.of(resultCode);
        }
    }

    public static void ne(Object expected, Object actual, String message) {
        if (Objects.equals(expected, actual)) {
            throw BusinessException.of(message);
        }
    }
}
