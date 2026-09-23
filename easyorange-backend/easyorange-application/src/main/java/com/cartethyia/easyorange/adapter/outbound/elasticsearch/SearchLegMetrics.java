package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * 检索双路（kNN / BM25）腿级打点 — 三个 ES 适配器共用一套指标名，改名只改这里。
 * <p>
 * {@code runLeg} 按设计吞掉单路异常（退化为另一路排名），失败在这里是<b>唯一可见面</b>：
 * 没有计数时腿挂掉表现为「结果变少」，只能靠 grep 日志发现（2026-09-23 排查实测假通过）。
 * <ul>
 *   <li>{@code easyorange.search.leg.calls{source,leg,outcome}} — 失败率按
 *       {@code outcome="failure"} 出数，降级率口径的腿级分量；</li>
 *   <li>{@code easyorange.search.leg.duration{source,leg,outcome}} — P50/P95/P99，
 *       腿超时（失败里慢的那部分）与正常腿延迟分开看。</li>
 * </ul>
 * source ∈ product / knowledge / asset，leg ∈ knn / bm25。
 */
@Component
public class SearchLegMetrics {

    private final MeterRegistry meterRegistry;

    public SearchLegMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String source, String leg, Duration elapsed, boolean success) {
        String outcome = success ? "success" : "failure";
        Counter.builder("easyorange.search.leg.calls")
                .description("Search recall leg invocations by source/leg/outcome")
                .tags("source", source, "leg", leg, "outcome", outcome)
                .register(meterRegistry)
                .increment();
        Timer.builder("easyorange.search.leg.duration")
                .description("Search recall leg latency by source/leg/outcome")
                .tags("source", source, "leg", leg, "outcome", outcome)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(elapsed);
    }
}
