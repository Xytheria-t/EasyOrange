package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import com.cartethyia.easyorange.ai.domain.model.AgentStepTrace;
import com.cartethyia.easyorange.ai.domain.port.AgentTracePort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 步级轨迹记录器 — 多步工具循环每轮落一条 {@code eo_agent_step_trace}，
 * 作为平均步数 / 降级率 / 步级延迟 p95 三个口径的数据源。
 * <p>
 * 记录失败只告警不抛出：轨迹是观测副产物，绝不能影响对话主链路（与 {@link AiCallLogRecorder} 同一取向）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTraceRecorder implements AgentTracePort {

    private static final String INSERT_SQL = """
            INSERT INTO eo_agent_step_trace
                (id, trace_id, session_id, user_id, step_index, tool, tool_input, thought, observation,
                 latency_ms, success, error_msg)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    @Override
    public void record(AgentStepTrace trace) {
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    idGenerator.generateId(),
                    trace.traceId(),
                    trace.sessionId(),
                    trace.userId(),
                    trace.stepIndex(),
                    trace.tool(),
                    truncate(trace.toolInput(), 512),
                    truncate(trace.thought(), 255),
                    truncate(trace.observation(), 512),
                    Math.max(trace.latencyMs(), 0),
                    trace.success() ? 1 : 0,
                    truncate(trace.errorMsg(), 512));
        } catch (Exception e) {
            log.warn(
                    "Agent step trace record failed (sessionId={}, step={}): {}",
                    trace.sessionId(),
                    trace.stepIndex(),
                    e.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
