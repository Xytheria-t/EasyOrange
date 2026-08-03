package com.cartethyia.easyorange.framework.event.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 领域事件可观测性指标服务 — 记录事件接收、处理结果、处理时延、DLQ 死信计数，
 * 暴露给 Prometheus 抓取（{@code /actuator/prometheus}）。
 * <p>
 * 与 {@code BusinessMetricsService} 模式一致，纯计数/计时，
 * 不包含业务语义。所有方法接受 {@code eventType} tag，便于按事件类型分维度切片。
 * <p>
 * 指标清单：
 * <ul>
 *   <li>{@code easyorange.events.received{type,outcome}} — 接收计数器（success/failure/duplicate/concurrent_skip）</li>
 *   <li>{@code easyorange.events.duration{type,outcome}} — 处理时延计时器（P50/P95/P99）</li>
 *   <li>{@code easyorange.events.dlq{type,reason}} — DLQ 死信计数器</li>
 * </ul>
 */
@Component
public class EventMetricsService {

    private static final String METRIC_RECEIVED = "easyorange.events.received";
    private static final String METRIC_DURATION = "easyorange.events.duration";
    private static final String METRIC_DLQ = "easyorange.events.dlq";

    private final MeterRegistry meterRegistry;

    public EventMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /** 事件接收 +1，outcome: success / failure / duplicate / concurrent_skip */
    public void recordReceived(String eventType, String outcome) {
        meterRegistry.counter(METRIC_RECEIVED, "type", eventType, "outcome", outcome).increment();
    }

    /** 启动计时器采样 */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /** 记录处理时延 */
    public void recordDuration(String eventType, Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder(METRIC_DURATION)
                .tag("type", eventType)
                .tag("outcome", outcome)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
    }

    /** DLQ 死信 +1，reason: max_retries / rejected / ttl_expired / unknown */
    public void recordDlq(String queue, String reason) {
        meterRegistry.counter(METRIC_DLQ, "queue", queue, "reason", reason).increment();
    }
}
