package com.cartethyia.easyorange.ai.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Agent 步骤事件 — SSE {@code step} 事件的载荷，前端据此做步骤可视化（正在查什么 / 为什么查）。
 *
 * @param step        步序（1 起，含 finish 轮）
 * @param tool        工具名（knowledge_search / product_search / product_detail / finish）
 * @param thought     模型决策理由（不超过 20 字）
 * @param observation 观察摘要（命中数 / 命中标题 / 详情摘要；finish 轮为 null）
 */
public record AgentStepView(int step, String tool, @Nullable String thought, @Nullable String observation) {}
