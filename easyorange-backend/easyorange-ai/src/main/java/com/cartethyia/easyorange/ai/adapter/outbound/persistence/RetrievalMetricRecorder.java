package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 检索指标采样落库 — 每条金标准检索用例一行 eo_retrieval_metric，
 * 按 run_id 聚合即可产出 hit@5 / MRR 趋势（AI dashboard 的 Judge 均分同源数据）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetrievalMetricRecorder {

    private static final String INSERT_SQL = """
            INSERT INTO eo_retrieval_metric (id, run_id, case_id, query_text, gold_doc_ids, hit_at_5, reciprocal_rank)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    public void record(
            String runId, String caseId, String query, String goldDocIds, boolean hit, double reciprocalRank) {
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    idGenerator.generateId(),
                    runId,
                    caseId,
                    query,
                    goldDocIds,
                    hit ? 1 : 0,
                    reciprocalRank);
        } catch (Exception e) {
            log.warn("Record retrieval metric failed, skip", e);
        }
    }
}
