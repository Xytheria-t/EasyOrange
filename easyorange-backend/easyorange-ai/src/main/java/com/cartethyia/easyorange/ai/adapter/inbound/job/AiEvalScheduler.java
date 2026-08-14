package com.cartethyia.easyorange.ai.adapter.inbound.job;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.service.AiJudge;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * LLM-as-Judge 离线评估 — 定时对 {@code eo_ai_call_log} 中未评审的成功 AI 调用打分。
 * <p>
 * 评审逻辑收敛在 {@link AiJudge}（与金标准集回归共用同一评审器）；本类只负责
 * 拉取候选、回写 judge_score / judge_comment。输出质量从「感觉还行」变成
 * 「可量化、可回归」：某个 prompt 版本改动后平均分变化可观测。
 * <p>
 * 默认关闭（easyorange.ai.eval.enabled=false），演示/生产按需开启。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEvalScheduler {

    private static final String QUERY_SQL = """
            SELECT id, scope, prompt_hash, response_text
            FROM eo_ai_call_log
            WHERE judge_score IS NULL AND success = 1
            ORDER BY created_at DESC
            LIMIT ?
            """;

    private static final String UPDATE_SQL =
            "UPDATE eo_ai_call_log SET judge_score = ?, judge_comment = ? WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final AiJudge aiJudge;
    private final AiProperties aiProperties;

    @Scheduled(cron = "${easyorange.ai.eval.cron:0 0 3 * * ?}")
    public void evaluateUnjudgedCalls() {
        if (!aiProperties.getEval().isEnabled()) {
            return;
        }
        int batchSize = aiProperties.getEval().getBatchSize();

        List<Map<String, Object>> candidates;
        try {
            candidates = jdbcTemplate.queryForList(QUERY_SQL, batchSize);
        } catch (Exception e) {
            log.warn("LLM-as-Judge: query candidates failed", e);
            return;
        }
        if (candidates.isEmpty()) {
            return;
        }

        int judged = 0;
        for (Map<String, Object> row : candidates) {
            String id = (String) row.get("id");
            String scope = (String) row.get("scope");
            String response = (String) row.get("response_text");
            try {
                var judgement = aiJudge.judge(scope, response).orElse(null);
                if (judgement == null) {
                    continue;
                }
                jdbcTemplate.update(UPDATE_SQL, judgement.score(), judgement.comment(), id);
                judged++;
            } catch (Exception e) {
                log.warn("LLM-as-Judge: evaluate call {} failed: {}", id, e.getMessage());
            }
        }
        if (judged > 0) {
            log.info("LLM-as-Judge: judged {} of {} unjudged calls", judged, candidates.size());
        }
    }
}
