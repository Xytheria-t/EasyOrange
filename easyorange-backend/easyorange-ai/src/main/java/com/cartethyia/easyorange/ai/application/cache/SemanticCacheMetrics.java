package com.cartethyia.easyorange.ai.application.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 语义缓存的观测副产物 — 命中率口径的唯一来源。
 *
 * <p><b>为什么必须有它</b>：语义缓存的成本模型是「先无条件付一次供应商 embedding 调用，再看命不命中」，
 * 未命中时这笔钱纯亏。没有命中率就无法判断 {@code similarity-threshold} 0.92 与 {@code max-entries} 200
 * 是调准了还是白付——常量拍脑袋调出来的链路无法论证，也无法在换模型（换模型必然改变向量分布与相似度分布）
 * 后判断阈值是否还成立。
 *
 * <p><b>口径</b>：命中率 = {@code hit / (hit + miss)}，<b>分母只由真实发生过的查找构成</b>。两个结局不进分母：
 * {@link Outcome#BYPASS} 是根本没查（forceFresh 绕过 / 缓存关闭 / embedding 不可用），
 * {@link Outcome#ERROR} 是查了但 Redis 故障 fail-open。把它们计进分母会让「缓存故障期间命中率暴跌」
 * 被误读成「阈值该调松了」，而故障期间恰恰是最不该据此改参数的时候（理由同
 * {@link com.cartethyia.easyorange.ai.application.chat.ToolCallLoopMetrics} 的故障哨兵不进轮数分布）。
 *
 * <p>埋点是横切关注点，缓存实现不该知道指标名与 tag 契约：本类构造期按枚举全集注册
 * （tag 取值是时序契约，显式固化，改枚举名不影响历史数据；预注册也让 {@code rate(...)} 告警从 t=0 有效），
 * 热路径只做一次 {@link EnumMap} 查找、零字符串拼接。观测失败绝不影响主链路。
 */
@Component
public class SemanticCacheMetrics {

    /** 指标名 — 公开给调用方登记同一指标的 bypass 分支（{@code forceFresh} 绕过发生在缓存实现之外）。 */
    public static final String CACHE_METRIC = "easyorange.ai.semantic.cache";

    /** 一次语义缓存查找的结局 — tag 取值即 {@link #getTag()}，改名会断掉历史序列。 */
    public enum Outcome {
        /** 命中：桶内有条目相似度超阈值，复用其响应，跳过一次 LLM 调用。 */
        HIT("hit"),
        /** 未命中：查找正常执行但无条目超阈值，本次仍会走完整 LLM 链路。 */
        MISS("miss"),
        /** 绕过：根本没查。三个来源——客户端 forceFresh、缓存开关关闭、embedding 模型不可用。 */
        BYPASS("bypass"),
        /** 故障：查了但失败，向量化抛异常或 Redis 不可用，fail-open。 */
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

    /**
     * 记一次故障穿透 — 只记 {@link Outcome#ERROR}，不进命中率分母。
     *
     * <p>与 {@code ToolCallLoopMetrics#recordLoopFailure} 同源：故障期间读到的低命中率不代表阈值需要调整，
     * 把故障算成 miss 会诱导运维在故障期间去动一个当时无法验证的参数。
     */
    public void recordFailure() {
        counters.get(Outcome.ERROR).increment();
    }
}
