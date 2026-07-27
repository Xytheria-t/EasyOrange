package com.cartethyia.easyorange.ai.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * AI 链路可观测性指标服务 — 记录多级缓存命中率、LLM/Vision 调用延迟、
 * 限流拒绝/降级/放行计数、Token 预算用量、Bulkhead 隔离拒绝，
 * 暴露给 Prometheus 抓取（/actuator/prometheus）。
 */
@Component
public class AiMetricsService {

    private final MeterRegistry meterRegistry;

    public AiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ── 缓存指标 ──────────────────────────────────────────────────

    /** Cache Hit */
    public void recordCacheHit(String scope) {
        meterRegistry.counter("easyorange.ai.cache", "scope", scope, "outcome", "hit").increment();
    }

    /** Cache Miss */
    public void recordCacheMiss(String scope) {
        meterRegistry.counter("easyorange.ai.cache", "scope", scope, "outcome", "miss").increment();
    }

    /** Stale 缓存命中（限流降级场景） */
    public void recordStaleServed(String scope) {
        meterRegistry.counter("easyorange.ai.cache", "scope", scope, "outcome", "stale").increment();
    }

    // ── 计时器 ────────────────────────────────────────────────────

    /** 启动计时 */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /** 记录 LLM 调用耗时 */
    public void recordLlmDuration(String scope, Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("easyorange.ai.llm.duration")
                .tag("scope", scope)
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    /** 记录 Vision 调用耗时 */
    public void recordVisionDuration(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("easyorange.ai.vision.duration")
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    // ── 限流指标 ──────────────────────────────────────────────────

    /** 限流拒绝 */
    public void recordRateLimitRejected(String scope) {
        meterRegistry.counter("easyorange.ai.ratelimit", "scope", scope, "outcome", "rejected").increment();
    }

    /** 限流时返回 stale 缓存 */
    public void recordRateLimitStaleServed(String scope) {
        meterRegistry.counter("easyorange.ai.ratelimit", "scope", scope, "outcome", "stale_served").increment();
    }

    /** 限流 fail-open（Redis 不可用） */
    public void recordRateLimitFailOpen(String scope) {
        meterRegistry.counter("easyorange.ai.ratelimit", "scope", scope, "outcome", "fail_open").increment();
    }

    // ── Token 预算指标 ────────────────────────────────────────────

    /**
     * 记录 Token 预算用量（每次 AI 调用后上报）。
     *
     * @param scenario    场景名（与 AiCallScope 枚举名对齐）
     * @param usedTokens  本次调用估算 token 数
     * @param dailyTotal  当日累计 token 数（含本次）
     */
    public void recordTokenBudgetUsage(String scenario, int usedTokens, long dailyTotal) {
        meterRegistry.counter("easyorange.ai.token_budget.usage",
                "scenario", scenario, "outcome", "consumed").increment(usedTokens);
        meterRegistry.gauge("easyorange.ai.token_budget.daily_total",
                io.micrometer.core.instrument.Tags.of("scenario", scenario),
                dailyTotal);
    }

    /** Token 预算超限（拒绝调用） */
    public void recordTokenBudgetExceeded(String scenario) {
        meterRegistry.counter("easyorange.ai.token_budget",
                "scenario", scenario, "outcome", "exceeded").increment();
    }

    // ── Bulkhead 隔离指标 ────────────────────────────────────────

    /** Bulkhead 满载拒绝（并发上限达到） */
    public void recordBulkheadRejected(String name) {
        meterRegistry.counter("easyorange.ai.bulkhead",
                "name", name, "outcome", "rejected").increment();
    }
}
