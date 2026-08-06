package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.Collection;

public class BizRequire {

    private BizRequire() {}

    // --- notNull ---

    public static void notNull(Object obj, String message) {
        if (obj == null) fail(message);
    }

    public static void notNull(Object obj, IResultCode resultCode) {
        if (obj == null) fail(resultCode);
    }

    // --- notBlank ---

    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) fail(message);
    }

    // --- notEmpty ---

    public static <T> void notEmpty(Collection<T> collection, String message) {
        if (collection == null || collection.isEmpty()) fail(message);
    }

    // --- requireTrue ---

    public static void requireTrue(boolean condition, String message) {
        if (!condition) fail(message);
    }

    public static void requireTrue(boolean condition, IResultCode resultCode) {
        if (!condition) fail(resultCode);
    }

    // --- private helpers ---

    private static void fail(String message) {
        throw BusinessException.of(message);
    }

    private static void fail(IResultCode resultCode) {
        throw BusinessException.of(resultCode);
    }
}
