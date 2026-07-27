package com.cartethyia.easyorange.framework.config.resilience4j;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.AmqpException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Resilience4j 配置 — 手动创建 Registry（不使用 spring-boot-starter，兼容 Spring Boot 4）。
 * <p>
 * 当前用途：
 * <ul>
 *   <li>Redis 操作熔断（CategoryCacheAdapter）</li>
 *   <li>AI LLM/Vision 调用重试 + 并发隔离（CachingLlmAdapter / CachingVisionAdapter）</li>
 *   <li>DB 查询熔断（dbQuery — 连接超时/查询超时）</li>
 *   <li>RabbitMQ 消息发布熔断（rabbitMQ — AMQP 连接异常）</li>
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
 * <p>
 * 默认 Bulkhead（隔离仓）配置：
 * <ul>
 *   <li>{@code aiLlm}：并发 8（LLM 调用较慢，限并发保护下游）</li>
 *   <li>{@code aiVision}：并发 4（Vision 调用更慢更耗资源）</li>
 *   <li>{@code dbHeavy}：并发 16（预留给重查询场景）</li>
 *   <li>最大等待时间：100ms，超时抛 {@code BulkheadFullException}（不阻塞调用线程）</li>
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

    private static final int BULKHEAD_MAX_WAIT_MS = 100;
    private static final int BULKHEAD_AI_LLM_CONCURRENCY = 8;
    private static final int BULKHEAD_AI_VISION_CONCURRENCY = 4;
    private static final int BULKHEAD_DB_HEAVY_CONCURRENCY = 16;

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

    /**
     * DB 查询熔断器 — 连接池耗尽 / 查询超时时开路，保护下游不被慢查询拖垮。
     * <p>
     * 记录异常：{@link CannotGetJdbcConnectionException}（连接池耗尽）、{@link QueryTimeoutException}（查询超时）
     */
    @Bean("dbQuery")
    public CircuitBreaker dbQueryCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = CircuitBreakerConfig.from(registry.getDefaultConfig())
                .recordExceptions(CannotGetJdbcConnectionException.class, QueryTimeoutException.class)
                .build();
        return registry.circuitBreaker("dbQuery", config);
    }

    /**
     * RabbitMQ 消息熔断器 — AMQP 连接异常时开路，防止消息发布反复失败拖垮请求线程。
     * <p>
     * 记录异常：{@link AmqpException}（连接拒绝 / 通道关闭等）
     */
    @Bean("rabbitMQ")
    public CircuitBreaker rabbitMqCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = CircuitBreakerConfig.from(registry.getDefaultConfig())
                .recordExceptions(AmqpException.class)
                .build();
        return registry.circuitBreaker("rabbitMQ", config);
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

    /**
     * Bulkhead 注册表 — 隔离 AI/DB 调用并发，防止单一慢下游耗尽线程池。
     * <p>
     * 三个具名 Bulkhead：
     * <ul>
     *   <li>{@code aiLlm}（并发 8）— DeepSeek LLM 调用</li>
     *   <li>{@code aiVision}（并发 4）— Qwen-VL 视觉调用</li>
     *   <li>{@code dbHeavy}（并发 16）— 预留给重查询场景</li>
     * </ul>
     * 超过并发上限时，新调用最多等待 100ms，超时抛 {@link io.github.resilience4j.bulkhead.BulkheadFullException}，
     * 调用方应捕获并走降级路径（如返回 stale 缓存）。
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry(MeterRegistry meterRegistry) {
        var config = BulkheadConfig.custom()
                .maxConcurrentCalls(BULKHEAD_AI_LLM_CONCURRENCY)
                .maxWaitDuration(Duration.ofMillis(BULKHEAD_MAX_WAIT_MS))
                .build();

        var registry = BulkheadRegistry.of(config);

        // 各 Bulkhead 独立并发上限 — 用自定义配置覆盖默认值
        var visionConfig = BulkheadConfig.from(config)
                .maxConcurrentCalls(BULKHEAD_AI_VISION_CONCURRENCY)
                .build();
        var dbConfig = BulkheadConfig.from(config)
                .maxConcurrentCalls(BULKHEAD_DB_HEAVY_CONCURRENCY)
                .build();

        registry.bulkhead("aiLlm", config);
        registry.bulkhead("aiVision", visionConfig);
        registry.bulkhead("dbHeavy", dbConfig);

        // 绑定 Micrometer 指标：自动暴露并发可用量 / 使用量
        TaggedBulkheadMetrics.ofBulkheadRegistry(registry)
                .bindTo(meterRegistry);

        return registry;
    }

    @Bean("aiLlm")
    public Bulkhead aiLlmBulkhead(BulkheadRegistry bulkheadRegistry) {
        return bulkheadRegistry.bulkhead("aiLlm");
    }

    @Bean("aiVision")
    public Bulkhead aiVisionBulkhead(BulkheadRegistry bulkheadRegistry) {
        return bulkheadRegistry.bulkhead("aiVision");
    }

    @Bean("dbHeavy")
    public Bulkhead dbHeavyBulkhead(BulkheadRegistry bulkheadRegistry) {
        return bulkheadRegistry.bulkhead("dbHeavy");
    }
}
