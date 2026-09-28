package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.port.query.AiCostReportPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 成本报表读仓储 — 在 {@code eo_ai_call_log} 上按场景聚合（token 用量降序，同量按调用次数）。
 * <p>
 * 用量口径（只认供应商真实回报、不估算）写在端口上；这里只负责把那条口径翻译成聚合 SQL。
 */
@Repository
@RequiredArgsConstructor
public class AiCostReportQueryRepository implements AiCostReportPort {

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

    @Override
    @Transactional(readOnly = true)
    public List<AiCostReportRow> report(int hours) {
        return jdbcTemplate.query(REPORT_SQL, rowMapper(), hours);
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
