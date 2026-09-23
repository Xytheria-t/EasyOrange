package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 调用日志记录器 — 每次 LLM/Embedding 调用落一条 {@code eo_ai_call_log}，
 * 作为**成本报表**的数据源。
 * <p>
 * 记录失败只告警不抛出：AI 调用日志是观测副产物，绝不能影响主链路。
 * <p>
 * {@code REQUIRES_NEW} 是硬要求：调用常被包在外层事务里，而检索链路的事务是
 * {@code @Transactional(readOnly = true)}——借用外层连接的 INSERT 会直接被拒
 *（{@code Connection is read-only}），SEMANTIC（搜索向量化）行曾因此一条都没落过库；
 * 挂起外层、开独立读写连接提交，也让调用日志与业务事务的成败解耦（LLM 花的钱已经发生）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCallLogRecorder implements AiCallLogPort {

    private static final String INSERT_SQL = """
            INSERT INTO eo_ai_call_log
                (id, scope, model, prompt_hash, response_text, latency_ms, token_input, token_output,
                 success, error_msg)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String scope,
            String model,
            String promptHash,
            String response,
            long latencyMs,
            int tokenInput,
            int tokenOutput,
            boolean success,
            @Nullable String errorMsg) {
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    idGenerator.generateId(),
                    scope,
                    model,
                    promptHash,
                    response,
                    latencyMs,
                    Math.max(tokenInput, 0),
                    Math.max(tokenOutput, 0),
                    success ? 1 : 0,
                    errorMsg != null && errorMsg.length() > 512 ? errorMsg.substring(0, 512) : errorMsg);
        } catch (Exception e) {
            log.warn("AI call log record failed (scope={}): {}", scope, e.getMessage());
        }
    }
}
