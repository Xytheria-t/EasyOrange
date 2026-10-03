package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.support.ToolCallLoopOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ToolCallLoopMetrics (循环观测副产物) -> 测试")
class ToolCallLoopMetricsTest {

    private SimpleMeterRegistry registry;
    private ToolCallLoopMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ToolCallLoopMetrics(registry);
    }

    @Test
    @DisplayName("构造期全集预注册 —— 结局与工具 tag 在任何一次调用发生前就已在时序里（rate 类告警从 t=0 有效）")
    void meters_registeredAtConstruction() {
        for (ToolCallLoopOutcome outcome : ToolCallLoopOutcome.values()) {
            assertThat(registry.counter("easyorange.ai.chat.loop", "outcome", outcome.getTag())
                            .count())
                    .isZero();
        }
        for (String tool : List.of(
                ChatTools.TOOL_KNOWLEDGE_SEARCH,
                ChatTools.TOOL_PRODUCT_SEARCH,
                ChatTools.TOOL_PRODUCT_DETAIL,
                ChatTools.TOOL_MARKET_PRICE_STATS,
                ChatTools.TOOL_COMPARE_ASSETS,
                ChatTools.TOOL_REMEMBER_PREFERENCE,
                "unknown")) {
            assertThat(registry.counter("easyorange.ai.chat.tool", "name", tool).count())
                    .isZero();
            assertThat(registry.timer("easyorange.ai.chat.tool.duration", "tool", tool)
                            .count())
                    .isZero();
        }
        assertThat(registry.summary("easyorange.ai.chat.steps").count()).isZero();
    }

    @Test
    @DisplayName("工具 tag 集 = 6 个可执行工具 + unknown；收敛工具 finish 不在内（无执行体，结局由 loop{outcome} 计）")
    void toolTagSet_excludesFinish() {
        assertThat(registry.getMeters())
                .filteredOn(
                        meter -> "easyorange.ai.chat.tool".equals(meter.getId().getName()))
                .extracting(meter -> meter.getId().getTag("name"))
                .containsExactlyInAnyOrder(
                        ChatTools.TOOL_KNOWLEDGE_SEARCH,
                        ChatTools.TOOL_PRODUCT_SEARCH,
                        ChatTools.TOOL_PRODUCT_DETAIL,
                        ChatTools.TOOL_MARKET_PRICE_STATS,
                        ChatTools.TOOL_COMPARE_ASSETS,
                        ChatTools.TOOL_REMEMBER_PREFERENCE,
                        "unknown");
        assertThat(registry.find("easyorange.ai.chat.tool")
                        .tag("name", ChatTools.TOOL_FINISH)
                        .counter())
                .isNull();
    }

    @Test
    @DisplayName("模型输出名单外的工具名 -> 落 unknown，不新增时序（防提示注入撑爆 tag 基数）")
    void recordTool_unknownNameFallsBackToUnknownTag() {
        metrics.recordTool("evil_tool", 12);

        assertThat(registry.counter("easyorange.ai.chat.tool", "name", "unknown")
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.getMeters())
                .filteredOn(
                        meter -> "easyorange.ai.chat.tool".equals(meter.getId().getName()))
                .hasSize(7);
    }

    @Test
    @DisplayName("recordLoop -> 结局计数 + 决策轮数分布（降级率与平均步数的口径来源）")
    void recordLoop_countsOutcomeAndRounds() {
        metrics.recordLoop(ToolCallLoopOutcome.FINISHED, 3);
        metrics.recordLoop(ToolCallLoopOutcome.STEP_LIMIT, 7);

        assertThat(registry.counter("easyorange.ai.chat.loop", "outcome", "finished")
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.counter("easyorange.ai.chat.loop", "outcome", "step_limit")
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.summary("easyorange.ai.chat.steps").count()).isEqualTo(2L);
        assertThat(registry.summary("easyorange.ai.chat.steps").totalAmount()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("recordLoopFailure -> 只记结局，不进轮数分布（记 0 会把平均步数的均值往 0 拽）")
    void recordLoopFailure_excludedFromSteps() {
        metrics.recordLoop(ToolCallLoopOutcome.FINISHED, 3);
        metrics.recordLoopFailure();

        assertThat(registry.counter("easyorange.ai.chat.loop", "outcome", "error")
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.summary("easyorange.ai.chat.steps").count()).isEqualTo(1L);
        assertThat(registry.summary("easyorange.ai.chat.steps").totalAmount()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("分位数不由客户端算 —— 走直方图分桶，metric 不带 quantile 维度（多副本下由 PromQL 聚合）")
    void percentilesNotComputedClientSide() {
        metrics.recordTool(ChatTools.TOOL_PRODUCT_SEARCH, 8);
        metrics.recordLoop(ToolCallLoopOutcome.FINISHED, 1);

        assertThat(registry.timer("easyorange.ai.chat.tool.duration", "tool", ChatTools.TOOL_PRODUCT_SEARCH)
                        .takeSnapshot()
                        .percentileValues())
                .isEmpty();
        assertThat(registry.summary("easyorange.ai.chat.steps").takeSnapshot().percentileValues())
                .isEmpty();
        assertThat(registry.getMeters()).noneMatch(meter -> meter.getId().getTag("quantile") != null);
    }
}
