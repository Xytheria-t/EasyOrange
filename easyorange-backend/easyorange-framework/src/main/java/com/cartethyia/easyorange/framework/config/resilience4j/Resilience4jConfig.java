package com.cartethyia.easyorange.framework.config.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Duration;

/**
 * Resilience4j 配置 — 手动创建 Registry（不使用 spring-boot-starter，兼容 Spring Boot 4）。
 * <p>
 * 当前用途：
 * <ul>
 *   <li>Redis 操作熔断（CategoryCacheAdapter）</li>
 * </ul>
 * <p>
 * 默认熔断配置：
 * <ul>
 *   <li>滑动窗口：10 次计数</li>
 *   <li>最小调用数：5</li>
 *   <li>失败率阈值：50%</li>
 *   <li>开路等待时间：60 秒后进入 Half-Open</li>
 *   <li>Half-Open 允许：3 次探测调用</li>
 * </ul>
 */
@AutoConfiguration
public class Resilience4jConfig {

    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final float FAILURE_RATE_THRESHOLD = 50;
    private static final int WAIT_DURATION_OPEN_STATE_SECONDS = 60;
    private static final int PERMITTED_CALLS_IN_HALF_OPEN = 3;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        var config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_OPEN_STATE_SECONDS))
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN)
                .recordExceptions(RedisConnectionFailureException.class, QueryTimeoutException.class)
                .ignoreExceptions(UnsupportedOperationException.class)
                .build();

        var registry = CircuitBreakerRegistry.of(config);

        // 绑定 Micrometer 指标：自动暴露 circuit breaker 状态 + 调用计数
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry)
                .bindTo(meterRegistry);

        return registry;
    }
}
