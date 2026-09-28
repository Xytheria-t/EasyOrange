package com.cartethyia.easyorange.ai.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Agent 步级轨迹 — 多步工具循环每轮决策落一行（含 finish 轮），是
 * 平均步数 / 降级率 / 步级延迟 p95 三个口径的数据源（表 {@code eo_agent_step_trace}）。
 * stepIndex 从 1 起；finish 轮无执行体：latencyMs 为 0、observation 为 null。
 */
public record AgentStepTrace(
        String traceId,
        @Nullable String sessionId,
        @Nullable String userId,
        int stepIndex,
        String tool,
        @Nullable String toolInput,
        @Nullable String thought,
        @Nullable String observation,
        long latencyMs,
        boolean success,
        @Nullable String errorMsg) {}
