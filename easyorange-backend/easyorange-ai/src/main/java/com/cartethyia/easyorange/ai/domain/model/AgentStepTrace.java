package com.cartethyia.easyorange.ai.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Agent 步级轨迹 — 多步工具循环每轮决策落一行（含 finish 轮），是
 * 平均步数 / 降级率 / 步级延迟 p95 三个口径的数据源（表 {@code eo_agent_step_trace}）。
 *
 * @param traceId     循环轨迹 ID（一次对话请求一个）
 * @param sessionId   会话 ID
 * @param userId      用户 ID（匿名对话为 null）
 * @param stepIndex   步序（1 起，含 finish 轮）
 * @param tool        工具名（knowledge_search / product_search / product_detail / finish）
 * @param toolInput   工具入参（检索词 / 资产 ID）
 * @param thought     模型决策理由
 * @param observation 观察摘要（命中数 / 命中标题 / 详情摘要）
 * @param latencyMs   该步耗时毫秒（finish 轮为 0）
 * @param success     工具是否执行成功
 * @param errorMsg    失败原因
 */
public record AgentStepTrace(
        String traceId,
        String sessionId,
        @Nullable String userId,
        int stepIndex,
        String tool,
        @Nullable String toolInput,
        @Nullable String thought,
        @Nullable String observation,
        long latencyMs,
        boolean success,
        @Nullable String errorMsg) {}
