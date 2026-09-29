package com.cartethyia.easyorange.ai.application.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 语义缓存的观测副产物 — 命中率口径的唯一来源：命中率 = {@code hit / (hit + miss)}，分母只由真实发生过的
 * 查找构成。{@link Outcome#BYPASS}（根本没查）与 {@link Outcome#ERROR}（查了但 Redis 故障 fail-open）不进分母 ——
 * 计进去会让「故障期间命中率暴跌」被误读成「阈值该调松」，而故障期恰恰最不该据此改参数。
 *
 * <p>没有它就调不动参数：成本模型是「先无条件付一次供应商 embedding 调用，再看命不命中」，未命中纯亏；换模型
 * 必然改变向量与相似度分布，没有命中率就无从判断 0.92 / 200 是否还成立。
 *
 * <p>埋点是横切关注点，缓存实现不该知道指标名与 tag 契约：本类构造期按枚举全集注册（tag 取值是时序契约，
 * 显式固化，改枚举名不影响历史数据；预注册也让 {@code rate(...)} 告警从 t=0 有效），热路径只做一次
 * {@link EnumMap} 查找、零字符串拼接。观测失败绝不影响主链路。
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

    /** 记一次故障穿透 — 只记 {@link Outcome#ERROR}，不进命中率分母；与 {@code ToolCallLoopMetrics#recordLoopFailure} 同源。 */
    public void recordFailure() {
        counters.get(Outcome.ERROR).increment();
    }
}
