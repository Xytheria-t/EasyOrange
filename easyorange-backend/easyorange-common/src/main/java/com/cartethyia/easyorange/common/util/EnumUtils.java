package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 枚举工具类
 * <p>
 * 提供通用的枚举查找、转换方法，避免在各枚举类中重复实现 fromCode 逻辑。
 * 高频调用场景推荐使用 {@code *Cached} 变体，内部使用 LRU 缓存查找结果。
 * </p>
 *
 * @author cartethyia
 */
public final class EnumUtils {

    private EnumUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final ConcurrentHashMap<String, Map<Integer, Object>> CODE_CACHE = new ConcurrentHashMap<>(16);

    private static final ConcurrentHashMap<String, Map<String, Object>> RESULT_CODE_CACHE = new ConcurrentHashMap<>(16);

    private static final int MAX_CACHE_SIZE_PER_ENUM = 128;

    private static <K, V> Map<K, V> createLruMap() {
        return Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE_PER_ENUM, 0.75f, true) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MAX_CACHE_SIZE_PER_ENUM;
            }
        });
    }

    public static <T> T fromCode(int code, T[] values, Function<T, Integer> codeExtractor) {
        return Stream.of(values)
                .filter(v -> codeExtractor.apply(v) == code)
                .findFirst()
                .orElse(null);
    }

    public static <T> Optional<T> fromCodeSafe(int code, T[] values, Function<T, Integer> codeExtractor) {
        return Stream.of(values)
                .filter(v -> codeExtractor.apply(v) == code)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromCodeCached(int code, T[] values, Function<T, Integer> codeExtractor) {
        String cacheKey = values.getClass().getComponentType().getName();
        Map<Integer, Object> cache = CODE_CACHE.computeIfAbsent(cacheKey, k -> createLruMap());
        return (T) cache.computeIfAbsent(code, c ->
                Stream.of(values).filter(v -> codeExtractor.apply(v) == c).findFirst().orElse(null));
    }

    public static <T extends IResultCode> T fromResultCode(String code, T[] values) {
        return Stream.of(values)
                .filter(v -> v.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public static <T extends IResultCode> Optional<T> fromResultCodeSafe(String code, T[] values) {
        return Stream.of(values)
                .filter(v -> v.getCode().equals(code))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public static <T extends IResultCode> T fromResultCodeCached(String code, T[] values) {
        String cacheKey = values.getClass().getComponentType().getName();
        Map<String, Object> cache = RESULT_CODE_CACHE.computeIfAbsent(cacheKey, k -> createLruMap());
        return (T) cache.computeIfAbsent(code, c ->
                Stream.of(values).filter(v -> v.getCode().equals(c)).findFirst().orElse(null));
    }

    public static <T extends Enum<T>> T fromName(String name, T[] values) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return Stream.of(values)
                .filter(v -> v.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static <T extends Enum<T>> Optional<T> fromNameSafe(String name, T[] values) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return Stream.of(values)
                .filter(v -> v.name().equals(name))
                .findFirst();
    }
}