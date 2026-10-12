package com.cartethyia.easyorange.ai.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * 工具步级轨迹 — 每次工具调用落一行（一轮可并行多调用；finish 收敛轮占位一行），是
 * 工具步数 / 步级延迟 p95 两个口径的数据源（表 {@code eo_tool_call_step_trace}）。
 * stepIndex 从 1 起；finish 轮无执行体：latencyMs 为 0、observation 为 null。
 */
public record ToolCallStepTrace(
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
