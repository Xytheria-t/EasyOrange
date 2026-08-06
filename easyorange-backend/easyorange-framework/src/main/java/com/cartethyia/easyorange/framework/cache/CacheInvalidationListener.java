package com.cartethyia.easyorange.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * L1 缓存跨节点失效广播 — 基于 Redis Pub/Sub。
 * <p>
 * 当一个节点执行 {@link MultiLevelCache#evict(String)} 时，通过 Redis Pub/Sub 发布失效消息，
 * 其他节点收到消息后失效本地 L1（Caffeine）缓存，保证多节点 L1 缓存一致性。
 * <p>
 * 消息格式：{@code prefix\u0001key}（String 序列化，不携带 Jackson 类型信息）。
 * <p>
 * 注册：每个 {@link MultiLevelCache} 实例在构造时调用 {@link #register(String, Cache)} 注册其 L1 缓存，
 * 以 {@code l2KeyPrefix} 作为标识。
 */
@Slf4j
@Component
public class CacheInvalidationListener {

    /** Pub/Sub 频道名 */
    public static final String CHANNEL = "eo:cache:invalidation";

    /** 分隔符 — 使用 \u0001（SOH 控制字符）避免与 cache key 中的冒号冲突 */
    private static final String SEPARATOR = "\u0001";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Cache<String, Object>> l1Caches = new ConcurrentHashMap<>();

    public CacheInvalidationListener(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 注册 L1 缓存 — MultiLevelCache 构造时调用。
     *
     * @param prefix  缓存前缀（l2KeyPrefix），作为缓存实例标识
     * @param l1Cache 本地 Caffeine L1 缓存
     */
    public void register(String prefix, Cache<String, Object> l1Cache) {
        l1Caches.put(prefix, l1Cache);
    }

    /**
     * 发布缓存失效消息到 Redis Pub/Sub。
     * <p>
     * 使用 {@link StringRedisTemplate} 发送纯字符串，String 序列化不携带类型信息，
     * 接收端无需处理序列化器差异。
     *
     * @param prefix 缓存前缀
     * @param key    缓存键
     */
    public void publishInvalidation(String prefix, String key) {
        try {
            redisTemplate.convertAndSend(CHANNEL, prefix + SEPARATOR + key);
        } catch (Exception e) {
            log.warn(
                    "action=cache_invalidation_publish_failed, prefix={}, key={}, error={}",
                    prefix,
                    key,
                    e.getMessage());
        }
    }

    /**
     * 消费缓存失效消息 — 由 {@code MessageListenerAdapter} 将消息体反序列化为 String 后调用。
     *
     * @param body 消息体（{@code prefix\u0001key}）
     */
    public void handleMessage(String body) {
        try {
            var idx = body.indexOf(SEPARATOR);
            if (idx < 0) {
                return;
            }
            var prefix = body.substring(0, idx);
            var key = body.substring(idx + 1);
            var l1Cache = l1Caches.get(prefix);
            if (l1Cache != null) {
                l1Cache.invalidate(key);
                log.debug("action=cache_invalidated_via_pubsub, prefix={}, key={}", prefix, key);
            }
        } catch (Exception e) {
            log.warn("action=cache_invalidation_receive_failed, error={}", e.getMessage());
        }
    }
}
