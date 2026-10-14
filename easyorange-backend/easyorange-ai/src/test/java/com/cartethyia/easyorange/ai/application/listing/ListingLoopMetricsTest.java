package com.cartethyia.easyorange.ai.application.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 发布链路指标契约 — 买家侧 {@code ChatLoopMetricsTest} 已覆盖基类 {@code ToolLoopMetricsSupport} 的全部行为，
 * 这里只钉发布侧自己的那份差异：{@code easyorange.ai.listing.*} 命名空间与 4 个可执行工具的 tag 封闭集。
 * 命名空间写错不会编译失败，只会让 Grafana 面板的查询静默落空，所以必须有断言。
 */
@DisplayName("ListingLoopMetrics (发布链路循环观测) -> 测试")
class ListingLoopMetricsTest {

    private SimpleMeterRegistry registry;
    private ListingLoopMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ListingLoopMetrics(registry);
    }

    @Test
    @DisplayName("命名空间是 easyorange.ai.listing.*，与买家侧 chat.* 各自独立、不与 chat 面板混读")
    void meters_registeredUnderListingNamespace() {
        for (ToolLoopOutcome outcome : ToolLoopOutcome.values()) {
            assertThat(registry.counter("easyorange.ai.listing.loop", "outcome", outcome.getTag())
                            .count())
                    .isZero();
        }
        assertThat(registry.summary("easyorange.ai.listing.rounds").count()).isZero();
        // 买家侧命名空间下不该出现任何 meter —— 混命名空间会让两条链路的降级率互相污染
        assertThat(registry.getMeters()).noneMatch(m -> m.getId().getName().startsWith("easyorange.ai.chat."));
    }

    @Test
    @DisplayName("工具 tag 集 = 4 个可执行工具 + unknown；finish 无执行体不入名单")
    void tools_taggedWithClosedSet() {
        assertThat(registry.getMeters())
                .filteredOn(m -> m.getId().getName().equals("easyorange.ai.listing.tool"))
                .extracting(m -> m.getId().getTag("name"))
                .containsExactlyInAnyOrder(
                        ListingTools.TOOL_KNOWLEDGE_SEARCH,
                        ListingTools.TOOL_PRODUCT_SEARCH,
                        ListingTools.TOOL_MARKET_PRICE_STATS,
                        ListingTools.TOOL_LIST_CATEGORIES,
                        "unknown");
    }

    @Test
    @DisplayName("名单外工具名落 unknown，不新增时序（防提示注入撑爆 tag 基数）")
    void recordTool_unknownNameFallsBackToUnknownTag() {
        metrics.recordTool("evil_tool", 5);

        assertThat(registry.counter("easyorange.ai.listing.tool", "name", "unknown")
                        .count())
                .isEqualTo(1);
        assertThat(registry.getMeters())
                .filteredOn(m -> m.getId().getName().equals("easyorange.ai.listing.tool"))
                .extracting(m -> m.getId().getTag("name"))
                .containsExactlyInAnyOrder(
                        ListingTools.TOOL_KNOWLEDGE_SEARCH,
                        ListingTools.TOOL_PRODUCT_SEARCH,
                        ListingTools.TOOL_MARKET_PRICE_STATS,
                        ListingTools.TOOL_LIST_CATEGORIES,
                        "unknown");
    }

    @Test
    @DisplayName("rounds 分布收的是发布链路口径（含 finish 轮，故障哨兵不入分布）")
    void rounds_recordsOnlyRealLoopExits() {
        metrics.recordLoop(ToolLoopOutcome.FINISHED, 4);
        metrics.recordLoopFailure();

        assertThat(registry.summary("easyorange.ai.listing.rounds").count()).isEqualTo(1L);
        assertThat(registry.summary("easyorange.ai.listing.rounds").totalAmount())
                .isEqualTo(4.0);
        assertThat(registry.counter("easyorange.ai.listing.loop", "outcome", "error")
                        .count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("闭环工具执行：名单内工具计数 + 耗时，两条 meter 同时落")
    void recordTool_knownToolRecordsCountAndDuration() {
        metrics.recordTool(ListingTools.TOOL_MARKET_PRICE_STATS, 8);

        assertThat(registry.counter("easyorange.ai.listing.tool", "name", ListingTools.TOOL_MARKET_PRICE_STATS)
                        .count())
                .isEqualTo(1);
        assertThat(registry.timer("easyorange.ai.listing.tool.duration", "tool", ListingTools.TOOL_MARKET_PRICE_STATS)
                        .count())
                .isEqualTo(1);
    }
}
