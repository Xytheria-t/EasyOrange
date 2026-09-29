package com.cartethyia.easyorange.framework.util;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 分布式限流器 — 基于 Redisson {@link RRateLimiter} 的令牌桶。
 * <p>
 * 不用 {@code increment + expire} 固定窗口：两次调用之间崩溃会让 key 无 TTL 永久限流，且固定窗口边界可放过 2× 流量；
 * 用 Redisson 内置实现而非手写 Lua，顺带避开 RedisTemplate 序列化器与 Lua {@code tonumber} 的兼容问题。
 * <p>
 * <b>fail-open</b>：Redis 异常时本方法抛出，由调用方（{@code RateLimitFilter} / {@code AiRateLimitInterceptor}）catch 后放行。
 * 补桶时脚本内已设 {@code PEXPIRE}，key 随窗口到期被 Redis 自动回收，无需手动清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedRateLimiter {

    private final RedissonClient redissonClient;

    /**
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
