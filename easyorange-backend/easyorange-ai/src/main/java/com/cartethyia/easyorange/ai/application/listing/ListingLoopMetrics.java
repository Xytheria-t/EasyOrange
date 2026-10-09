package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.toolcall.ToolCallLoopOutcome;
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * 发布链路循环的观测副产物 — 与买家侧 {@code ToolCallLoopMetrics} 同构、不同命名空间（easyorange.ai.listing.*）：
 * 两条链路的步数分布 / 降级率要分开看（发布链路每步都是定价与合规的必经查证，降级语义更重）。
 * <p>
 * 埋点是横切关注点，编排器不该知道指标名与 tag 契约：构造期按枚举全集预注册（tag 是时序契约），热路径只做
 * {@link EnumMap} 查找。观测失败绝不影响主链路。
 */
@Component
public class ListingLoopMetrics implements ToolLoopListener {

    private static final String LOOP_METRIC = "easyorange.ai.listing.loop";
    private static final String STEPS_METRIC = "easyorange.ai.listing.steps";
    private static final String TOOL_METRIC = "easyorange.ai.listing.tool";
    private static final String TOOL_DURATION_METRIC = "easyorange.ai.listing.tool.duration";

    /** 工具指标 tag 封闭集 — 名单即 {@link ListingTools} 的 4 个可执行工具；finish 无执行体，不入名单（它的分布由 loop{outcome=finished} 计）。 */
    private enum TrackedTool {
        KNOWLEDGE_SEARCH(ListingTools.TOOL_KNOWLEDGE_SEARCH),
        PRODUCT_SEARCH(ListingTools.TOOL_PRODUCT_SEARCH),
        MARKET_PRICE_STATS(ListingTools.TOOL_MARKET_PRICE_STATS),
        LIST_CATEGORIES(ListingTools.TOOL_LIST_CATEGORIES),
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

    private final Map<ToolCallLoopOutcome, Counter> loopCounters;
    private final DistributionSummary stepsSummary;
    private final Map<TrackedTool, Counter> toolCounters;
    private final Map<TrackedTool, Timer> toolTimers;

    public ListingLoopMetrics(MeterRegistry meterRegistry) {
        this.loopCounters = new EnumMap<>(ToolCallLoopOutcome.class);
        for (ToolCallLoopOutcome outcome : ToolCallLoopOutcome.values()) {
            loopCounters.put(outcome, meterRegistry.counter(LOOP_METRIC, "outcome", outcome.getTag()));
        }
        this.stepsSummary = DistributionSummary.builder(STEPS_METRIC)
                .description("发布链路每次循环出口的决策轮数（含 finish 轮，故障哨兵不入分布）")
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

    @Override
    public void recordLoop(ToolCallLoopOutcome outcome, int rounds) {
        loopCounters.get(outcome).increment();
        stepsSummary.record(rounds);
    }

    @Override
    public void recordLoopFailure() {
        loopCounters.get(ToolCallLoopOutcome.ERROR).increment();
    }

    @Override
    public void recordTool(String toolName, long latencyMs) {
        TrackedTool tracked = TrackedTool.fromName(toolName);
        toolCounters.get(tracked).increment();
        toolTimers.get(tracked).record(latencyMs, TimeUnit.MILLISECONDS);
    }
}
