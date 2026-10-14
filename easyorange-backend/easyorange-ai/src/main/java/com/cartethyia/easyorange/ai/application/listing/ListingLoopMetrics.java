package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopListener;
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopMetricsSupport;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 发布链路循环指标 — {@code easyorange.ai.listing.*} 命名空间，与买家侧同构、不同命名空间（埋点形状见
 * {@link ToolLoopMetricsSupport}）。两条链路的轮数分布 / 降级率分开看：发布链路每轮都是定价与合规的必经查证，
 * 降级语义更重。{@code finish} 无执行体，不入工具名单（其分布由 {@code loop{outcome=finished}} 计）。
 */
@Component
public class ListingLoopMetrics extends ToolLoopMetricsSupport implements ToolLoopListener {

    private static final List<String> TRACKED_TOOLS = List.of(
            ListingTools.TOOL_KNOWLEDGE_SEARCH,
            ListingTools.TOOL_PRODUCT_SEARCH,
            ListingTools.TOOL_MARKET_PRICE_STATS,
            ListingTools.TOOL_LIST_CATEGORIES);

    public ListingLoopMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry, "easyorange.ai.listing", TRACKED_TOOLS, "发布链路每次循环出口的决策轮数（含 finish 轮，故障哨兵不入分布）");
    }
}
