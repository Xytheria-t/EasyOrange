package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 调用日志记录器 — 每次 LLM/Embedding 调用落一条 {@code eo_ai_call_log}，
 * 作为 LLM-as-Judge 离线评估的数据源。
 * <p>
 * 记录失败只告警不抛出：AI 调用日志是观测副产物，绝不能影响主链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCallLogRecorder {

    private static final String INSERT_SQL = """
            INSERT INTO eo_ai_call_log
                (id, scope, model, prompt_hash, response_text, latency_ms, success, error_msg)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    public void record(
            String scope,
            String model,
            String promptHash,
            String response,
            long latencyMs,
            boolean success,
            String errorMsg) {
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    idGenerator.generateId(),
                    scope,
                    model,
                    promptHash,
                    response,
                    latencyMs,
                    success ? 1 : 0,
                    errorMsg != null && errorMsg.length() > 512 ? errorMsg.substring(0, 512) : errorMsg);
        } catch (Exception e) {
            log.warn("AI call log record failed (scope={}): {}", scope, e.getMessage());
        }
    }
}
