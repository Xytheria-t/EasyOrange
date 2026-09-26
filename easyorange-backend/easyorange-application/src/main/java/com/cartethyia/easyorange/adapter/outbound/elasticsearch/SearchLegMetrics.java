package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
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
 * tag 空间封闭（source × leg × outcome = 3 × 2 × 2，见 {@link Source} / {@link Leg}），
 * 构造期按全集注册（12 组 counter + timer），热路径只查表不查注册表。
 */
@Component
public class SearchLegMetrics {

    /** source tag 封闭集 — 与三个共用 adapter 一一对应（搜索页商品 / 知识库 RAG / 对话找货）。 */
    public enum Source {
        PRODUCT("product"),
        KNOWLEDGE("knowledge"),
        ASSET("asset");

        private final String tag;

        Source(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    /** leg tag 封闭集 — 双路召回的两条腿。 */
    public enum Leg {
        KNN("knn"),
        BM25("bm25");

        private final String tag;

        Leg(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    private enum Outcome {
        SUCCESS("success"),
        FAILURE("failure");

        private final String tag;

        Outcome(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    /** 单个 (source, leg) 组合的全套 meter —— success/failure 各一对，构造期注册。 */
    private record LegMeters(
            Counter successCalls, Counter failureCalls, Timer successDuration, Timer failureDuration) {}

    private final Map<Source, Map<Leg, LegMeters>> metersBySource = new EnumMap<>(Source.class);

    public SearchLegMetrics(MeterRegistry meterRegistry) {
        for (Source source : Source.values()) {
            var byLeg = new EnumMap<Leg, LegMeters>(Leg.class);
            for (Leg leg : Leg.values()) {
                byLeg.put(leg, new LegMeters(
                        counter(meterRegistry, source, leg, Outcome.SUCCESS),
                        counter(meterRegistry, source, leg, Outcome.FAILURE),
                        timer(meterRegistry, source, leg, Outcome.SUCCESS),
                        timer(meterRegistry, source, leg, Outcome.FAILURE)));
            }
            metersBySource.put(source, byLeg);
        }
    }

    public void record(Source source, Leg leg, Duration elapsed, boolean success) {
        LegMeters meters = metersBySource.get(source).get(leg);
        if (success) {
            meters.successCalls().increment();
            meters.successDuration().record(elapsed);
        } else {
            meters.failureCalls().increment();
            meters.failureDuration().record(elapsed);
        }
    }

    private static Counter counter(MeterRegistry registry, Source source, Leg leg, Outcome outcome) {
        return Counter.builder("easyorange.search.leg.calls")
                .description("Search recall leg invocations by source/leg/outcome")
                .tags("source", source.tag(), "leg", leg.tag(), "outcome", outcome.tag())
                .register(registry);
    }

    private static Timer timer(MeterRegistry registry, Source source, Leg leg, Outcome outcome) {
        return Timer.builder("easyorange.search.leg.duration")
                .description("Search recall leg latency by source/leg/outcome")
                .tags("source", source.tag(), "leg", leg.tag(), "outcome", outcome.tag())
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
