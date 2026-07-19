package com.cartethyia.easyorange.framework.web.idempotency;

import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 Idempotency-Key 幂等服务。
 * <p>
 * 原理：先检查 Redis 中是否已缓存此 key 的成功响应，
 * 未缓存则执行业务操作并原子性写入缓存（SETNX），
 * 确保并发场景下首个完成的请求决定返回结果。
 * </p>
 * <p>
 * 执行中抛出异常 → 不缓存，重试不受影响。
 * Redis 不可用 → fail-open，请求透传（降级为无幂等保护）。
 * </p>
 */
@Slf4j
@Service
@ConditionalOnClass(RedisTemplate.class)
public class RedisIdempotencyService implements IdempotencyService {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final IdempotencyProperties properties;

    public RedisIdempotencyService(RedisTemplate<Object, Object> redisTemplate, IdempotencyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(String key, long ttlSeconds, IdempotentOperation<T> operation) throws Throwable {
        var redisKey = redisKey(key);

        // 快速路径：已有缓存
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                log.debug("action=idempotency_cache_hit, key={}", key);
                return (T) cached;
            }
        } catch (Exception e) {
            log.warn("action=idempotency_cache_read_error, key={}", key, e);
            // fail-open：Redis 不可用时降级为正常执行
            return operation.execute();
        }

        // 执行业务操作
        T result;
        try {
            result = operation.execute();
        } catch (Throwable t) {
            // 异常不缓存，重试可重新执行
            log.debug("action=idempotency_exec_failed, key={}", key);
            throw t;
        }

        // 写入缓存（SETNX 防止并发覆盖）
        try {
            Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(redisKey, result, ttlSeconds > 0 ? ttlSeconds : properties.getDefaultTtlSeconds(), TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(wasSet)) {
                // 并发请求先写入了，使用它的结果
                Object existing = redisTemplate.opsForValue().get(redisKey);
                if (existing != null) {
                    return (T) existing;
                }
            }
        } catch (Exception e) {
            log.warn("action=idempotency_cache_write_error, key={}", key, e);
            // 缓存写入失败不影响业务结果
        }

        return result;
    }

    @Override
    public boolean isProcessed(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey(key)));
        } catch (Exception e) {
            log.warn("action=idempotency_check_error, key={}", key, e);
            return false;
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(redisKey(key));
            log.debug("action=idempotency_evict, key={}", key);
        } catch (Exception e) {
            log.warn("action=idempotency_evict_error, key={}", key, e);
        }
    }

    private String redisKey(String key) {
        return properties.getKeyPrefix() + ":" + key;
    }
}
