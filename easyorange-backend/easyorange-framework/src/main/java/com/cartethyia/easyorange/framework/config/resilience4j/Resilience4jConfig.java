package com.cartethyia.easyorange.framework.config.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Resilience4j 配置 — 手动创建 Registry（不使用 spring-boot-starter，兼容 Spring Boot 4）。
 * <p>
 * 当前用途：
 * <ul>
 *   <li>Redis 操作熔断（CategoryCacheAdapter）</li>
 *   <li>AI LLM/Vision 调用重试（CachingLlmAdapter / CachingVisionAdapter）</li>
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
 * <p>
 * 默认重试配置：
 * <ul>
 *   <li>最大重试次数：3（首次调用 + 2 次重试）</li>
 *   <li>指数退避：500ms 初始，2x 倍增</li>
 *   <li>重试异常：RestClientException（网络超时、5xx）</li>
 *   <li>忽略异常：IllegalArgumentException（程序错误不重试）</li>
 * </ul>
 */
@AutoConfiguration
public class Resilience4jConfig {

    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final float FAILURE_RATE_THRESHOLD = 50;
    private static final int WAIT_DURATION_OPEN_STATE_SECONDS = 60;
    private static final int PERMITTED_CALLS_IN_HALF_OPEN = 3;

    private static final int RETRY_MAX_ATTEMPTS = 3;
    private static final int RETRY_INITIAL_INTERVAL_MS = 500;
    private static final double RETRY_MULTIPLIER = 2.0;

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

    @Bean
    public RetryRegistry retryRegistry() {
        var intervalFn = IntervalFunction.ofExponentialBackoff(
                Duration.ofMillis(RETRY_INITIAL_INTERVAL_MS), RETRY_MULTIPLIER);
        var config = RetryConfig.custom()
                .maxAttempts(RETRY_MAX_ATTEMPTS)
                .intervalFunction(intervalFn)
                .retryExceptions(RestClientException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        var registry = RetryRegistry.of(config);
        // 使用默认配置创建具名 Retry 实例（与默认配置一致，但可独立调优）
        registry.retry("aiLlm", config);
        registry.retry("aiVision", config);

        return registry;
    }

    @Bean("aiLlm")
    public Retry aiLlmRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("aiLlm");
    }

    @Bean("aiVision")
    public Retry aiVisionRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("aiVision");
    }
}
