package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalCaseMetric;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalRun;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricQueryPort;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检索指标回看读仓储 — 在 {@code eo_retrieval_metric} 上按 {@code run_id} 聚合（写侧见
 * {@code RetrievalMetricRecorder}）。放在 application 模块：管理端只读统计走组合根装配，与
 * {@code JdbcAiListingAdoptionAdapter} 同一层。
 * <p>
 * <b>评测线在 SQL 里按 case_id 前缀过滤而不是查出来再判</b>：两条线（retr-* 真实 embedding /
 * asset-* 合成向量）共用一张表，聚合前不分开就会把两种语料空间的命中率算成一个数。
 * <p>
 * 批次排序用 {@code created_at DESC} 而非 run_id（UUID v7 无字典序含义，按 ID 排等于随机序）。
 */
@Repository
@RequiredArgsConstructor
public class JdbcRetrievalMetricQueryAdapter implements RetrievalMetricQueryPort {

    private static final String RUNS_SQL = """
            SELECT run_id,
                   COUNT(*)                        AS case_count,
                   COALESCE(SUM(hit_at_5), 0)      AS hit_count,
                   COALESCE(SUM(reciprocal_rank), 0) AS rr_sum,
                   MIN(created_at)                 AS created_at
            FROM eo_retrieval_metric
            WHERE case_id LIKE ?
            GROUP BY run_id
            ORDER BY MIN(created_at) DESC
            LIMIT ?
            """;

    private static final String CASES_COUNT_SQL = "SELECT COUNT(*) FROM eo_retrieval_metric WHERE run_id = ?";

    /** 未命中的排前面：回看时先看到问题用例，命中位次升序让「排第一的」也聚在一起。 */
    private static final String CASES_SQL = """
            SELECT case_id, query_text, gold_doc_ids, hit_at_5, reciprocal_rank
            FROM eo_retrieval_metric
            WHERE run_id = ?
            ORDER BY hit_at_5 ASC, reciprocal_rank DESC, case_id ASC
            LIMIT ? OFFSET ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<RetrievalEvalRun> recentRuns(RetrievalEvalLine line, int limit) {
        return jdbcTemplate.query(
                RUNS_SQL,
                (rs, rowNum) -> {
                    int caseCount = rs.getInt("case_count");
                    // 分母取本批用例数：未命中的用例 rr=0 也进分母，漏掉它们等于把指标算成「命中的那些的平均」
                    double rrSum = rs.getDouble("rr_sum");
                    return new RetrievalEvalRun(
                            rs.getString("run_id"),
                            line,
                            caseCount,
                            rs.getInt("hit_count"),
                            caseCount == 0 ? 0 : rs.getInt("hit_count") * 1.0 / caseCount,
                            caseCount == 0 ? 0 : rrSum / caseCount,
                            rs.getTimestamp("created_at") == null
                                    ? null
                                    : rs.getTimestamp("created_at")
                                            .toLocalDateTime()
                                            .toString());
                },
                line.caseIdPrefix() + "%",
                limit);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RetrievalEvalCaseMetric> pageCases(String runId, int pageNum, int pageSize) {
        Long total = jdbcTemplate.queryForObject(CASES_COUNT_SQL, Long.class, runId);
        if (total == null || total == 0) {
            return PageResult.empty(pageNum, pageSize);
        }
        var records = jdbcTemplate.query(
                CASES_SQL,
                (rs, rowNum) -> new RetrievalEvalCaseMetric(
                        rs.getString("case_id"),
                        rs.getString("query_text"),
                        rs.getString("gold_doc_ids"),
                        rs.getInt("hit_at_5") == 1,
                        rs.getDouble("reciprocal_rank")),
                runId,
                pageSize,
                (pageNum - 1) * pageSize);
        return PageResult.of(records, total, pageNum, pageSize);
    }
}
