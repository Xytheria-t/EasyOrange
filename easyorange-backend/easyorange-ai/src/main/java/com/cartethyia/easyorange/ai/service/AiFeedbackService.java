package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.adapter.outbound.persistence.GoldenSetExportService;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 输出反馈入库（👍/👎 反馈飞轮）— 观测类数据，仿 eo_ai_call_log 用 JdbcTemplate 直写，
 * 失败只告警不阻塞主链路。导出逻辑见 {@link GoldenSetExportService}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiFeedbackService {

    private static final String INSERT_SQL = """
            INSERT INTO eo_ai_feedback (id, scope, query_text, response_text, helpful, comment, call_log_id, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    public void record(
            String scope, String question, String answer, boolean helpful, String comment, String callLogId) {
        try {
            String userId = SecurityContextUtil.getCurrentUserId().orElse(null);
            jdbcTemplate.update(
                    INSERT_SQL,
                    idGenerator.generateId(),
                    scope != null ? scope : "chat",
                    question,
                    answer,
                    helpful ? 1 : 0,
                    comment,
                    callLogId,
                    userId);
        } catch (Exception e) {
            log.warn("Record AI feedback failed, skip", e);
        }
    }
}
