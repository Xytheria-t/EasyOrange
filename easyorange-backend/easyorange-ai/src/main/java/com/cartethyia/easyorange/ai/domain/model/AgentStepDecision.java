package com.cartethyia.easyorange.ai.domain.model;

/**
 * ReAct 步骤决策器输出 — 多步工具循环里每一轮模型选择「下一个工具 + 参数 + 简短理由」。
 *
 * @param thought    本步理由（不超过 20 字，SSE step 事件与 trace 落库都会带出，前端步骤可视化展示）
 * @param tool       knowledge_search（平台规则）/ product_search（在售资产）/ product_detail（资产详情，需 productId）/ finish（信息足够，收敛生成）
 * @param query      改写后的检索关键词（product_detail 与 finish 时为空）
 * @param productId  资产 ID（仅 product_detail 使用，必须来自此前 product_search 的观察）
 * @param preference 提取到的用户偏好（无则 null，沿用单步 ReAct 时代的画像提取）
 */
public record AgentStepDecision(
        String thought, String tool, String query, String productId, Preference preference) {

    public record Preference(String key, String value) {}
}
