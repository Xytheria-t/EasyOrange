package com.cartethyia.easyorange.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Stream 常用操作封装
 * <p>
 * 提供去重、分组转 Map、安全 Collect 等常用 Stream 操作，
 * 避免在业务代码中重复编写冗长的 Stream 链。
 * </p>
 *
 * @author cartethyia
 */
public final class StreamUtils {

    private StreamUtils() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== 去重 ====================

    /**
     * 根据指定属性去重（保留第一个出现的元素）
     *
     * <pre>{@code
     * // 用法示例：按 userId 去重
     * List<User> distinct = list.stream()
     *     .filter(StreamUtils.distinctByKey(User::getId))
     *     .collect(Collectors.toList());
     * }</pre>
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        return t -> seen.add(keyExtractor.apply(t));
    }

    // ==================== 集合转换 ====================

    /**
     * 将集合按指定 key 提取器转为 Map
     * <p>
     * 如果存在重复 key，保留第一个值。
     * </p>
     */
    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyExtractor) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.toMap(
                        keyExtractor,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 将集合按指定 key/value 提取器转为 Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection,
                                            Function<T, K> keyExtractor,
                                            Function<T, V> valueExtractor) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.toMap(
                        keyExtractor,
                        valueExtractor,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 将集合按指定 key 提取器分组
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> keyExtractor) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }
        return collection.stream().collect(Collectors.groupingBy(keyExtractor));
    }

    // ==================== 安全提取 ====================

    /**
     * 安全获取集合的第一个元素
     */
    public static <T> T getFirst(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        return collection.iterator().next();
    }

    /**
     * 安全获取集合的最后一个元素
     */
    public static <T> T getLast(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        if (collection instanceof List<?> list) {
            return (T) list.get(list.size() - 1);
        }
        return collection.stream().reduce((first, second) -> second).orElse(null);
    }

    // ==================== 过滤 ====================

    /**
     * 过滤集合中的 null 元素
     */
    public static <T> List<T> filterNull(Collection<T> collection) {
        if (collection == null) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 过滤集合中的 null 和空白字符串
     */
    public static List<String> filterBlank(Collection<String> collection) {
        if (collection == null) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }

    // ==================== 映射提取 ====================

    /**
     * 提取集合中某属性的列表
     */
    public static <T, R> List<R> mapToList(Collection<T> collection, Function<T, R> mapper) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyList();
        }
        return collection.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 提取集合中某属性的 Set（去重）
     */
    public static <T, R> Set<R> mapToSet(Collection<T> collection, Function<T, R> mapper) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }
        return collection.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
