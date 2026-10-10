package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ToolCallStepTrace;

/**
 * 工具步级轨迹端口 — 多步工具循环每轮落一条 {@code eo_tool_call_step_trace}（实现在 adapter/outbound）；
 * 轨迹是观测副产物，记录失败只告警不抛出，绝不影响对话主链路（同 {@link AiCallLogPort} 取向）。
 */
public interface ToolCallStepTracePort {

    void record(ToolCallStepTrace trace);
}
