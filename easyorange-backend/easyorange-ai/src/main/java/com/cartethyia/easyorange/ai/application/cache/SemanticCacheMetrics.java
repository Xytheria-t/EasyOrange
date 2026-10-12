package com.cartethyia.easyorange.ai.application.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 语义缓存的观测副产物 — 命中率口径唯一来源：{@code hit / (hit + miss)}，只有真实发生过的查找进分母；
 * {@link Outcome#BYPASS}（根本没查）与 {@link Outcome#ERROR}（Redis 故障 fail-open）不计入，否则
 * 「故障期命中率暴跌」会被误读成「阈值该调松」—— 而故障期恰恰最不该据此改参数。
 *
 * <p>没它调不动参数：未命中也得先付一次供应商 embedding，换模型必然改变向量与相似度分布，命中率是判断
 * 0.92 / 200 是否还成立的唯一依据。埋点属横切：缓存实现不持指标名与 tag 契约，构造期按枚举全集预注册；
 * 热路径只做一次 {@link EnumMap} 查找，观测失败不影响主链路。
 */
@Component
public class SemanticCacheMetrics {

    /** 指标名 — 公开给调用方登记同一指标的 bypass 分支（{@code forceFresh} 绕过发生在缓存实现之外）。 */
    public static final String CACHE_METRIC = "easyorange.ai.semantic.cache";

    /** 一次语义缓存查找的结局 — tag 取值即 {@link #getTag()}，改名会断掉历史序列。 */
    public enum Outcome {
        HIT("hit"),
        MISS("miss"),
        /** 绕过：根本没查。三个来源——客户端 forceFresh、缓存开关关闭、embedding 模型不可用。 */
        BYPASS("bypass"),
        ERROR("error");

        private final String tag;

        Outcome(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    private final Map<Outcome, Counter> counters;

    public SemanticCacheMetrics(MeterRegistry meterRegistry) {
        this.counters = new EnumMap<>(Outcome.class);
        for (Outcome outcome : Outcome.values()) {
            counters.put(outcome, meterRegistry.counter(CACHE_METRIC, "outcome", outcome.getTag()));
        }
    }

    public void record(Outcome outcome) {
        counters.get(outcome).increment();
    }

    /** 记一次故障穿透 — 只记 {@link Outcome#ERROR}，不进命中率分母；与 {@code ChatLoopMetrics#recordLoopFailure} 同源。 */
    public void recordFailure() {
        counters.get(Outcome.ERROR).increment();
    }
}
