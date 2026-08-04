package com.cartethyia.easyorange.framework.util;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 分布式限流器 — 基于 Redisson {@link RRateLimiter} 的令牌桶实现。
 * <p>
 * 替代 {@code RedisTemplate.increment() + expire()} 的固定窗口方案，解决两个核心问题：
 * <ol>
 *   <li><b>原子性缺口</b>：{@code increment} 和 {@code expire} 是两个独立 Redis 调用，
 *       进程在两者之间崩溃会导致 key 无 TTL → 永久限流。
 *       Redisson 内部用 Lua 脚本保证取桶/补桶/扣桶原子化。</li>
 *   <li><b>固定窗口边界突刺</b>：固定窗口在边界处可放过 2× 流量。
 *       令牌桶以恒定速率补充令牌，平滑流量。</li>
 * </ol>
 * <p>
 * <b>STP 原则</b>：Redisson 已引入（分布式锁 RLock），{@link RRateLimiter} 是其内置令牌桶实现，
 * 无需手写 Lua 脚本（避免 RedisTemplate 序列化器与 Lua {@code tonumber} 的兼容问题）。
 * <p>
 * <b>fail-open 策略</b>：本方法在 Redis 异常时抛出异常，由调用方决定是否放行。
 * {@code RateLimitFilter} 和 {@code AiRateLimitInterceptor} 均在 catch 中放行。
 * <p>
 * <b>Key 清理</b>：Redisson 的 {@code RRateLimiter} 内部 Lua 脚本在补桶时设置 {@code PEXPIRE}，
 * 窗口到期后 Redis 自动回收 key，无需手动清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedRateLimiter {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取 1 个令牌。
     *
     * @param key           限流键（建议带业务前缀，如 {@code eo:rate:ip:1.2.3.4})
     * @param maxRequests   窗口内最大请求数（令牌桶容量）
     * @param windowSeconds 窗口大小（秒）
     * @return {@code true} 获得令牌（放行）；{@code false} 令牌耗尽（限流）
     * @throws org.redisson.client.RedisException Redis 不可用时抛出，调用方应 fail-open
     */
    public boolean tryAcquire(String key, long maxRequests, long windowSeconds) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // trySetRate 是幂等的 — 仅在首次调用时设置速率配置，后续调用为 no-op
        rateLimiter.trySetRate(RateType.OVERALL, maxRequests, Duration.ofSeconds(windowSeconds));
        return rateLimiter.tryAcquire(1);
    }
}
