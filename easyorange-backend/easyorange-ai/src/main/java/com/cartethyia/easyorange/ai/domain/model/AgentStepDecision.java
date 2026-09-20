package com.cartethyia.easyorange.ai.domain.model;

/**
 * 一步工具决策 — 原生 tool call 的工具名与原始参数 JSON，外加解析后的参数视图
 * （trace 落库 / SSE step 事件消费这些字段，参数 schema 见 {@code AgentTools} 的 {@code @Tool} 注解）。
 * <p>
 * 工具名与 {@code arguments} 都不在 arguments JSON 内部（前者来自 tool call 的 function name，后者就是
 * 原始串本身），Jackson 解析时二者为 null，由 {@link #withToolCall} 补入 —— 解析目标与补全分开，
 * 让「模型返回的参数不合 schema」只表现为参数字段缺失，而不是解析整体失败。
 *
 * @param thought 本步理由（不超过 20 字，SSE step 事件与 trace 落库都会带出，前端步骤可视化展示）
 * @param tool knowledge_search（平台规则）/ product_search（在售资产）/ product_detail（资产详情，需 productId）/ finish（信息足够，收敛生成）
 * @param query 改写后的检索关键词（product_detail 与 finish 时为空）
 * @param productId 资产 ID（仅 product_detail 使用，必须来自此前 product_search 的观察）
 * @param preferenceKey 用户偏好类别（仅 finish 轮带出：condition / price_range / style / location，无则 null）
 * @param preferenceValue 偏好的具体值（无则 null）
 * @param arguments 原生 tool call 的原始参数 JSON（工具执行直接交给 ToolCallback，trace / SSE 不落）
 */
public record AgentStepDecision(
        String thought,
        String tool,
        String query,
        String productId,
        String preferenceKey,
        String preferenceValue,
        String arguments) {

    /** 补入工具名与原始参数 JSON（二者都不在 arguments JSON 里）。 */
    public AgentStepDecision withToolCall(String tool, String arguments) {
        return new AgentStepDecision(thought, tool, query, productId, preferenceKey, preferenceValue, arguments);
    }
}
