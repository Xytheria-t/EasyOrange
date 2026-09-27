package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/**
 * 一步工具决策 — 原生 tool call 的工具名与原始参数 JSON，外加解析后的参数视图
 * （trace 落库 / SSE step 事件消费这些字段；各参数的取值约束见 {@code AgentTools} 的
 * {@code @ToolParam} 描述，与此处字段同源，不重写）。
 * <p>
 * 工具名与 {@code arguments} 都不在 arguments JSON 内部（前者来自 tool call 的 function name，
 * 后者就是原始串本身），Jackson 解析时二者为 null，由 {@link #withToolCall} 补入 ——
 * 解析目标与补全分开，让「模型返回的参数不合 schema」只表现为参数字段缺失，而不是解析整体失败。
 */
public record AgentStepDecision(
        String thought,
        String tool,
        String query,
        String productId,
        List<String> productIds,
        String preferenceKey,
        String preferenceValue,
        String arguments) {

    /** 补入工具名与原始参数 JSON（二者都不在 arguments JSON 里）。 */
    public AgentStepDecision withToolCall(String tool, String arguments) {
        return new AgentStepDecision(
                thought, tool, query, productId, productIds, preferenceKey, preferenceValue, arguments);
    }
}
