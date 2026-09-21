package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * AI 成本报表 — 按场景聚合 {@code eo_ai_call_log} 的调用次数、token 用量、平均耗时与失败次数。
 * <p>
 * 只统计**供应商真实回报的用量**：embedding 接口不返回 usage、流式未带 usage 的调用一律记 0，
 * 不在这里估算。估算口径留给预算器（它按场景上限兜底是为了让日限额不被绕过），
 * 但成本报表里混入估算值会让人把「没测到」当成「不花钱」——比缺数据更危险。
 */
@Service
@RequiredArgsConstructor
public class AiCostReportService {

    /** 时间窗上限 30 天：再宽的窗口在单表上就是全表扫描，且超出「近况」的语义。 */
    static final int MAX_WINDOW_HOURS = 24 * 30;

    private static final String REPORT_SQL = """
            SELECT scope,
                   COUNT(*)                                                        AS calls,
                   COALESCE(SUM(token_input), 0)                                   AS token_input,
                   COALESCE(SUM(token_output), 0)                                  AS token_output,
                   COALESCE(ROUND(AVG(latency_ms)), 0)                             AS avg_latency_ms,
                   COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END), 0)       AS failures
            FROM eo_ai_call_log
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? HOUR)
            GROUP BY scope
            ORDER BY (COALESCE(SUM(token_input), 0) + COALESCE(SUM(token_output), 0)) DESC, calls DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<AiCostReportRow> report(int hours) {
        return jdbcTemplate.query(REPORT_SQL, rowMapper(), clampWindow(hours));
    }

    static int clampWindow(int hours) {
        if (hours <= 0) {
            return 24;
        }
        return Math.min(hours, MAX_WINDOW_HOURS);
    }

    /** 独立出来便于直接测试映射（不必为了断言一列去 mock 整个 JdbcTemplate 调用链）。 */
    static RowMapper<AiCostReportRow> rowMapper() {
        return (rs, rowNum) -> new AiCostReportRow(
                rs.getString("scope"),
                rs.getLong("calls"),
                rs.getLong("token_input"),
                rs.getLong("token_output"),
                rs.getLong("avg_latency_ms"),
                rs.getLong("failures"));
    }
}
