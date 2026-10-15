package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopListener;
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopMetricsSupport;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 买家找货链路的循环指标 —— {@code easyorange.ai.chat.*} 命名空间；轮数分布与降级率与发布链路分开看，两条链路
 * 各自与自身基线比较。
 * <p>
 * 在链路里的位置：实现 {@link ToolLoopListener}，由 {@code ChatToolLoop} 装配进内核的 Spec —— 内核在每次出口、
 * 每个工具步回调它。埋点形状与 tag 口径见 {@link ToolLoopMetricsSupport}。
 */
@Component
public class ChatLoopMetrics extends ToolLoopMetricsSupport implements ToolLoopListener {

    /**
     * 工具指标 tag 封闭集 — 名单即 {@link ChatTools} 的 6 个可执行 {@code @Tool} 名，名单外一律记 {@code unknown}
     * （tag 取模型输出开集，一次提示注入就能撑爆时序基数）。
     * <p>
     * {@code finish} <b>不在名单里</b>：它没有执行体（收敛轮在执行前被拦截），计入只会多一条恒为零的序列，
     * 而它的分布已由 {@code chat.loop{outcome=finished}} 精确计数。
     */
    private static final List<String> TRACKED_TOOLS = List.of(
            ChatTools.TOOL_KNOWLEDGE_SEARCH,
            ChatTools.TOOL_PRODUCT_SEARCH,
            ChatTools.TOOL_PRODUCT_DETAIL,
            ChatTools.TOOL_MARKET_PRICE_STATS,
            ChatTools.TOOL_COMPARE_ASSETS,
            ChatTools.TOOL_REMEMBER_PREFERENCE);

    public ChatLoopMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry, "easyorange.ai.chat", TRACKED_TOOLS, "每次循环出口的决策轮数（含 finish 轮，故障哨兵不入分布）");
    }
}
