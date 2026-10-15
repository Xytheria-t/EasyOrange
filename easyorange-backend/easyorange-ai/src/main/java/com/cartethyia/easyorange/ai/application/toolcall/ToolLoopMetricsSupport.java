package com.cartethyia.easyorange.ai.application.toolcall;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 工具循环指标基类 —— 买家找货与卖家发布两条链路共用一套埋点形状，只换命名空间与工具名单。
 * <p>
 * 在链路里的位置：被两条链路的指标类继承（{@code ChatLoopMetrics} / {@code ListingLoopMetrics}），它们实现
 * {@link ToolLoopListener} 后装进内核的 Spec。
 * <p>
 * 埋点是横切关注点，编排器不该知道指标名与 tag 契约：构造期按名单全集注册（tag 取模型输出开集，一次提示注入就能撑爆
 * 时序基数，故名单外一律记 {@code unknown}），热路径只做 map 查找、零拼接。分位数走 {@code publishPercentileHistogram}
 * 而非 {@code publishPercentiles}：后者在 Prometheus 下是客户端算的 quantile gauge，跨副本不可聚合（生产多副本，直方图
 * 由 PromQL 按 {@code le} 分桶聚合）；观测失败绝不影响主链路 —— 调用方只传算好的结局与耗时，本类不抛异常、不反查配置。
 */
public abstract class ToolLoopMetricsSupport implements ToolLoopListener {

    private static final String UNKNOWN_TAG = "unknown";

    private final Map<ToolLoopOutcome, Counter> loopCounters;
    private final DistributionSummary roundsSummary;
    private final Map<String, Counter> toolCounters;
    private final Map<String, Timer> toolTimers;

    /**
     * 注册三组指标：循环结局计数、轮数分布、工具计数与耗时。
     *
     * @param meterRegistry 指标注册表（测试传 {@code SimpleMeterRegistry}）
     * @param metricPrefix 指标名前缀（如 {@code easyorange.ai.chat}）
     * @param trackedTools 工具 tag 封闭集 —— 不含 {@code finish}（无执行体，收敛轮在执行前被拦截，计入只会多一条恒零
     *                     序列，其分布已由 {@code loop{outcome=finished}} 精确计数）
     * @param roundsDescription 轮数分布 description（两条链路的分位数语义不同，文案分开写）
     */
    protected ToolLoopMetricsSupport(
            MeterRegistry meterRegistry, String metricPrefix, List<String> trackedTools, String roundsDescription) {
        this.loopCounters = new EnumMap<>(ToolLoopOutcome.class);
        for (ToolLoopOutcome outcome : ToolLoopOutcome.values()) {
            loopCounters.put(outcome, meterRegistry.counter(metricPrefix + ".loop", "outcome", outcome.getTag()));
        }
        this.roundsSummary = DistributionSummary.builder(metricPrefix + ".rounds")
                .description(roundsDescription)
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.toolCounters = new LinkedHashMap<>();
        this.toolTimers = new LinkedHashMap<>();
        for (String tool : trackedTools) {
            registerTool(meterRegistry, metricPrefix, tool);
        }
        // 名单外的落点同样预注册：否则 rate(...) 类告警要等第一次调用后才有效
        registerTool(meterRegistry, metricPrefix, UNKNOWN_TAG);
    }

    private void registerTool(MeterRegistry meterRegistry, String metricPrefix, String tool) {
        toolCounters.put(tool, meterRegistry.counter(metricPrefix + ".tool", "name", tool));
        toolTimers.put(
                tool,
                Timer.builder(metricPrefix + ".tool.duration")
                        .tag("tool", tool)
                        .publishPercentileHistogram()
                        .register(meterRegistry));
    }

    /** 记一次循环的结局与决策轮数 —— 结局含降级归因，轮数是「平均步数」口径的来源。 */
    @Override
    public void recordLoop(ToolLoopOutcome outcome, int rounds) {
        loopCounters.get(outcome).increment();
        roundsSummary.record(rounds);
    }

    /**
     * 记一次故障穿透 —— 只记结局，不进轮数分布：这类请求没有循环出口、轮数不可知，记 0 会把「平均步数」往 0 拽，而故障
     * 爆发恰恰最需要读这条曲线。分母因此只由真实出口构成，与 loop 计数器的非 error 之和一致。
     */
    @Override
    public void recordLoopFailure() {
        loopCounters.get(ToolLoopOutcome.ERROR).increment();
    }

    /** 记一次工具执行：调用计数（名单外落 unknown）+ 执行耗时。 */
    @Override
    public void recordTool(String toolName, long latencyMs) {
        Counter counter = toolCounters.getOrDefault(toolName, toolCounters.get(UNKNOWN_TAG));
        Timer timer = toolTimers.getOrDefault(toolName, toolTimers.get(UNKNOWN_TAG));
        counter.increment();
        timer.record(latencyMs, TimeUnit.MILLISECONDS);
    }
}
