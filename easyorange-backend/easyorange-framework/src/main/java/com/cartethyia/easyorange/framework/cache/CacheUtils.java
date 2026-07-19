package com.cartethyia.easyorange.framework.cache;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.NumberUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Redis 缓存工具方法集合。
 * <p>
 * 替代已移除的 {@link RedisCache} 中仍有价值的操作：
 * <ul>
 *   <li>{@link #cast(Object, Class)} — 类型安全的缓存读取（支持 Number 跨类型转换）</li>
 *   <li>{@link #scan(RedisTemplate, String)} — 基于 SCAN 的键模式匹配（非阻塞替代 KEYS）</li>
 * </ul>
 * </p>
 */
public final class CacheUtils {

    private CacheUtils() {
    }

    /**
     * 类型安全的缓存值转换（支持 Number 跨类型转换）。
     * <p>
     * {@link org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer} 在反序列化时
     * 可能返回 Integer 而调用方期望 Long（或反之），此方法自动转换已知的 Number 子类型。
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value, Class<T> type) {
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);
        if (value instanceof Number n && Number.class.isAssignableFrom(type)) {
            return (T) NumberUtils.convertNumberToTargetClass(n, (Class<? extends Number>) type);
        }
        throw new ClassCastException("Cannot cast " + value.getClass().getName() + " to " + type.getName());
    }

    /**
     * 使用 SCAN 命令查找匹配模式的键（非阻塞，替代 KEYS）。
     *
     * @param template RedisTemplate 实例
     * @param pattern  键模式（如 {@code eo:category:*})
     * @return 匹配的键集合，不会返回 null
     */
    public static Set<String> scan(RedisTemplate<Object, Object> template, String pattern) {
        Set<String> result = new HashSet<>();
        template.execute((RedisCallback<Set<String>>) connection -> {
            try (Cursor<byte[]> cursor = connection.keyCommands()
                    .scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return result;
        });
        return result;
    }
}
