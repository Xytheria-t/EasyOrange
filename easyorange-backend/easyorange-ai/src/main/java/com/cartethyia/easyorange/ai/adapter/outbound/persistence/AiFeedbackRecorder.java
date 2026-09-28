package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import com.cartethyia.easyorange.ai.domain.port.AiFeedbackPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 输出反馈记录器 — 落一条 {@code eo_ai_feedback}（id 在这里生成，与 {@link AiCallLogRecorder} 同源）。
 * <p>
 * 记录失败只告警不抛出：反馈是观测副产物，用户点一次「有用」不该让这次请求失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiFeedbackRecorder implements AiFeedbackPort {

    private static final String INSERT_SQL = """
            INSERT INTO eo_ai_feedback (id, scope, query_text, response_text, helpful, comment, call_log_id, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    @Override
    public void record(
            String scope,
            String question,
            String answer,
            boolean helpful,
            @Nullable String comment,
            @Nullable String callLogId,
            @Nullable String userId) {
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    idGenerator.generateId(),
                    scope,
                    question,
                    answer,
                    helpful ? 1 : 0,
                    comment,
                    callLogId,
                    userId);
        } catch (Exception e) {
            log.warn("AI feedback record failed, skip (scope={}): {}", scope, e.getMessage());
        }
    }
}
