package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.Collection;
import org.jetbrains.annotations.Contract;

public class BizRequire {

    private BizRequire() {}

    // --- notNull ---

    // 校验通过时返回入参（Guava/Objects.requireNonNull 式赋值）；契约供 Qodana/IDE 推断 null-after-call
    @Contract(value = "null, _ -> fail")
    public static <T> T notNull(T obj, String message) {
        if (obj == null) fail(message);
        return obj;
    }

    @Contract(value = "null, _ -> fail")
    public static <T> T notNull(T obj, IResultCode resultCode) {
        if (obj == null) fail(resultCode);
        return obj;
    }

    // --- notBlank ---

    // 校验通过时返回入参
    @Contract(value = "null, _ -> fail")
    public static String notBlank(String str, String message) {
        if (str == null || str.isBlank()) fail(message);
        return str;
    }

    // --- notEmpty ---

    // 校验通过时返回原集合并保持具体类型（List/Set...），供赋值/链式调用
    @Contract(value = "null, _ -> fail")
    public static <T extends Collection<?>> T notEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) fail(message);
        return collection;
    }

    // --- requireTrue ---

    @Contract(value = "false, _ -> fail")
    public static void requireTrue(boolean condition, String message) {
        if (!condition) fail(message);
    }

    @Contract(value = "false, _ -> fail")
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
