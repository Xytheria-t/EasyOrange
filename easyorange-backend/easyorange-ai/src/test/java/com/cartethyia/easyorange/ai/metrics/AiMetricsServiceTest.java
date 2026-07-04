package com.cartethyia.easyorange.ai.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiMetricsService 测试")
class AiMetricsServiceTest {

    private SimpleMeterRegistry registry;
    private AiMetricsService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new AiMetricsService(registry);
    }

    @Test
    @DisplayName("recordCacheHit 递增 hit 计数器")
    void recordCacheHit() {
        service.recordCacheHit("PRICING");
        var counter = registry.find("easyorange.ai.cache").tag("scope", "PRICING").tag("outcome", "hit").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordCacheMiss 递增 miss 计数器")
    void recordCacheMiss() {
        service.recordCacheMiss("REVIEW");
        var counter = registry.find("easyorange.ai.cache").tag("scope", "REVIEW").tag("outcome", "miss").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordStaleServed 递增 stale 计数器")
    void recordStaleServed() {
        service.recordStaleServed("QA");
        var counter = registry.find("easyorange.ai.cache").tag("scope", "QA").tag("outcome", "stale").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordLlmDuration 记录 LLM 调用耗时")
    void recordLlmDuration() {
        var sample = service.startTimer();
        service.recordLlmDuration("PRICING", sample, "success");
        var timer = registry.find("easyorange.ai.llm.duration").tag("scope", "PRICING").tag("outcome", "success").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("recordVisionDuration 记录 Vision 调用耗时")
    void recordVisionDuration() {
        var sample = service.startTimer();
        service.recordVisionDuration(sample, "success");
        var timer = registry.find("easyorange.ai.vision.duration").tag("outcome", "success").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("recordRateLimitRejected 递增 rejected 计数器")
    void recordRateLimitRejected() {
        service.recordRateLimitRejected("PRICING");
        var counter = registry.find("easyorange.ai.ratelimit").tag("scope", "PRICING").tag("outcome", "rejected").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordRateLimitStaleServed 递增 stale_served 计数器")
    void recordRateLimitStaleServed() {
        service.recordRateLimitStaleServed("COPY");
        var counter = registry.find("easyorange.ai.ratelimit").tag("scope", "COPY").tag("outcome", "stale_served").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordRateLimitFailOpen 递增 fail_open 计数器")
    void recordRateLimitFailOpen() {
        service.recordRateLimitFailOpen("QA");
        var counter = registry.find("easyorange.ai.ratelimit").tag("scope", "QA").tag("outcome", "fail_open").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("同一 scope+outcome 多次调用复用同一 Counter 实例")
    void counterReused() {
        service.recordCacheHit("PRICING");
        service.recordCacheHit("PRICING");
        var counter = registry.find("easyorange.ai.cache").tag("scope", "PRICING").tag("outcome", "hit").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }
}
