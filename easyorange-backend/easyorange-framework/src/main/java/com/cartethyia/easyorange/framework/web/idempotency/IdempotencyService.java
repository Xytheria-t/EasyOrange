package com.cartethyia.easyorange.framework.web.idempotency;

import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 基于 Redis 的 Idempotency-Key 幂等服务（原 RedisIdempotencyService 收敛为直接类）。
 * <p>
 * 原理：先抢「处理锁」（SETNX），只有抢到锁的请求才执行业务操作并缓存结果；
 * 并发重复请求作为输家轮询等待赢家的结果，确保同一 key 的业务操作只执行一次。
 * </p>
 * <p>
 * 业务操作抛异常 → 不缓存、释放锁，重试可重新执行。
 * 持有者崩溃（锁超时）→ 其它请求可重新抢锁执行。
 * Redis 不可用 → fail-open，请求透传（降级为无幂等保护）。
 * </p>
 */
@Slf4j
@Service
@ConditionalOnClass(RedisTemplate.class)
public class IdempotencyService {

    private static final String LOCK_MARKER = "1";

    private final RedisTemplate<Object, Object> redisTemplate;
    private final IdempotencyProperties properties;

    public IdempotencyService(RedisTemplate<Object, Object> redisTemplate, IdempotencyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(String key, long ttlSeconds, IdempotentOperation<T> operation) throws Exception {
        long resultTtl = ttlSeconds > 0 ? ttlSeconds : properties.getDefaultTtlSeconds();
        String resultKey = redisKey(key);
        String lockKey = lockKey(key);

        try {
            // 1. 快速路径：之前已完成并缓存了结果
            Object cached = getOrUnavailable(resultKey, key);
            if (cached != null) {
                return (T) cached;
            }

            // 2. 抢锁执行，或作为输家等待赢家结果
            long deadline = System.currentTimeMillis() + properties.getLockTtlSeconds() * 1000L;
            while (true) {
                if (tryAcquireLock(lockKey, key)) {
                    try {
                        // 抢到锁后复查，避免等待期间已被前一个赢家写入
                        Object done = getOrUnavailable(resultKey, key);
                        if (done != null) {
                            return (T) done;
                        }
                        T result = operation.execute(); // 仅赢家执行；业务异常向上抛、不缓存
                        cacheResult(resultKey, result, resultTtl, key);
                        return result;
                    } finally {
                        releaseLock(lockKey);
                    }
                }

                // 输家：轮询等赢家写入结果
                Object result = getOrUnavailable(resultKey, key);
                if (result != null) {
                    return (T) result;
                }
                if (System.currentTimeMillis() >= deadline) {
                    // 锁到期仍无结果（持有者崩溃）→ 兜底降级执行
                    log.warn("action=idempotency_lock_timeout, key={}", key);
                    return operation.execute();
                }
                waitBeforePoll();
            }
        } catch (RedisUnavailableException e) {
            // Redis 不可用 → fail-open：降级为直接执行，无幂等保护
            log.warn("action=idempotency_fail_open, key={}", key, e.getCause());
            return operation.execute();
        }
    }

    /** 读缓存；Redis 不可用抛 {@link RedisUnavailableException} 以触发 fail-open。 */
    private Object getOrUnavailable(String redisKey, String key) {
        try {
            return redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            throw new RedisUnavailableException(e);
        }
    }

    /** 抢处理锁；Redis 不可用抛 {@link RedisUnavailableException} 以触发 fail-open。 */
    private boolean tryAcquireLock(String lockKey, String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate
                    .opsForValue()
                    .setIfAbsent(lockKey, LOCK_MARKER, properties.getLockTtlSeconds(), TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RedisUnavailableException(e);
        }
    }

    /** 写结果缓存；失败仅记日志，不影响已执行的业务结果。 */
    private void cacheResult(String resultKey, Object result, long ttl, String key) {
        try {
            redisTemplate.opsForValue().set(resultKey, result, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("action=idempotency_cache_write_error, key={}", key, e);
        }
    }

    /** 释放处理锁；失败仅记日志。 */
    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("action=idempotency_lock_release_error, key={}", lockKey, e);
        }
    }

    private void waitBeforePoll() throws InterruptedException {
        try {
            Thread.sleep(properties.getLockPollIntervalMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private String redisKey(String key) {
        return properties.getKeyPrefix() + ":" + key;
    }

    private String lockKey(String key) {
        return redisKey(key) + ":lock";
    }

    /** 标记 Redis 不可用，触发统一的 fail-open 降级。 */
    private static final class RedisUnavailableException extends RuntimeException {
        RedisUnavailableException(Throwable cause) {
            super(cause);
        }
    }
}