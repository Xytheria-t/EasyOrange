package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopListener;
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopOutcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * 工具调用循环的观测副产物 — 四组指标：循环结局 / 决策轮数 / 工具调用分布 / 工具执行耗时。
 * <p>
 * 埋点是横切关注点，编排器不该知道指标名与 tag 契约：构造期按枚举全集注册（tag 值是时序契约、改枚举名不改 tag，也让
 * {@code rate(...)} 类告警从 t=0 有效），热路径只做 {@link EnumMap} 查找、零拼接。
 * <p>
 * 分位数走 {@code publishPercentileHistogram} 而非 {@code publishPercentiles}：后者在 Prometheus 下是客户端算的 quantile
 * gauge，跨副本不可聚合（生产多副本，直方图由 PromQL 按 {@code le} 分桶聚合）；观测失败绝不影响主链路 —— 调用方只传算好的
 * 结局与耗时，本类不抛异常、不反查配置。
 */
@Component
public class ToolCallLoopMetrics implements ToolLoopListener {

    private static final String LOOP_METRIC = "easyorange.ai.chat.loop";
    private static final String STEPS_METRIC = "easyorange.ai.chat.steps";
    private static final String TOOL_METRIC = "easyorange.ai.chat.tool";
    /** 工具执行耗时 —— 只包住工具执行本身，不含本轮决策的模型调用（决策调用耗时在 eo_ai_call_log 与 OTel span 里）。 */
    private static final String TOOL_DURATION_METRIC = "easyorange.ai.chat.tool.duration";

    /**
     * 工具指标 tag 封闭集 — 名单即 {@link ChatTools} 的 6 个可执行 {@code @Tool} 名，名单外一律记 {@code unknown}
     * （tag 取模型输出开集，一次提示注入就能撑爆时序基数）。
     * <p>
     * {@code finish} <b>不在名单里</b>：它没有执行体（收敛轮在执行前被拦截），计入只会多一条恒为零的序列，
     * 而它的分布已由 {@code chat.loop{outcome=finished}} 精确计数。
     */
    private enum TrackedTool {
        KNOWLEDGE_SEARCH(ChatTools.TOOL_KNOWLEDGE_SEARCH),
        PRODUCT_SEARCH(ChatTools.TOOL_PRODUCT_SEARCH),
        PRODUCT_DETAIL(ChatTools.TOOL_PRODUCT_DETAIL),
        MARKET_PRICE_STATS(ChatTools.TOOL_MARKET_PRICE_STATS),
        COMPARE_ASSETS(ChatTools.TOOL_COMPARE_ASSETS),
        REMEMBER_PREFERENCE(ChatTools.TOOL_REMEMBER_PREFERENCE),
        UNKNOWN("unknown");

        private final String tag;

        TrackedTool(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }

        static TrackedTool fromName(String tool) {
            for (TrackedTool tracked : values()) {
                if (tracked.tag.equals(tool)) {
                    return tracked;
                }
            }
            return UNKNOWN;
        }
    }

    private final Map<ToolLoopOutcome, Counter> loopCounters;
    private final DistributionSummary stepsSummary;
    private final Map<TrackedTool, Counter> toolCounters;
    private final Map<TrackedTool, Timer> toolTimers;

    public ToolCallLoopMetrics(MeterRegistry meterRegistry) {
        this.loopCounters = new EnumMap<>(ToolLoopOutcome.class);
        for (ToolLoopOutcome outcome : ToolLoopOutcome.values()) {
            loopCounters.put(outcome, meterRegistry.counter(LOOP_METRIC, "outcome", outcome.getTag()));
        }
        this.stepsSummary = DistributionSummary.builder(STEPS_METRIC)
                .description("每次循环出口的决策轮数（含 finish 轮，故障哨兵不入分布）")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.toolCounters = new EnumMap<>(TrackedTool.class);
        this.toolTimers = new EnumMap<>(TrackedTool.class);
        for (TrackedTool tracked : TrackedTool.values()) {
            toolCounters.put(tracked, meterRegistry.counter(TOOL_METRIC, "name", tracked.tag()));
            toolTimers.put(
                    tracked,
                    Timer.builder(TOOL_DURATION_METRIC)
                            .tag("tool", tracked.tag())
                            .publishPercentileHistogram()
                            .register(meterRegistry));
        }
    }

    /** 记一次循环的结局与决策轮数 —— 结局含降级归因，轮数是「平均步数」口径的来源。 */
    @Override
    public void recordLoop(ToolLoopOutcome outcome, int rounds) {
        loopCounters.get(outcome).increment();
        stepsSummary.record(rounds);
    }

    /**
     * 记一次故障穿透 —— 只记结局，不进轮数分布：这类请求没有循环出口、轮数不可知，记 0 会把「平均步数」往 0 拽，而故障
     * 爆发恰恰最需要读这条曲线。分母因此只由真实出口构成，与 loop 计数器的非 error 之和一致。
     */
    @Override
    public void recordLoopFailure() {
        loopCounters.get(ToolLoopOutcome.ERROR).increment();
    }

    /** 记一次工具执行：调用计数（tag 封闭集外落 unknown）+ 执行耗时。 */
    @Override
    public void recordTool(String toolName, long latencyMs) {
        TrackedTool tracked = TrackedTool.fromName(toolName);
        toolCounters.get(tracked).increment();
        toolTimers.get(tracked).record(latencyMs, TimeUnit.MILLISECONDS);
    }
}
