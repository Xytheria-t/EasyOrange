package com.cartethyia.easyorange.ai.adapter.inbound.job;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * LLM-as-Judge 离线评估 — 定时对 {@code eo_ai_call_log} 中未评审的成功 AI 调用打分。
 * <p>
 * 用 ChatModel 当评审员（Judge），按「相关性 / 完整性 / 格式规范 / 语气」四维给
 * 1-5 分 + 一句评语，回写 judge_score / judge_comment。输出质量从「感觉还行」
 * 变成「可量化、可回归」：某个 prompt 版本改动后平均分变化可观测。
 * <p>
 * 默认关闭（easyorange.ai.eval.enabled=false），演示/生产按需开启。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEvalScheduler {

    private static final String JUDGE_SYSTEM_PROMPT = """
            你是 AI 输出质量评审员（Judge）。请对下面的 AI 助手回答打分。
            评分标准（1-5 分）：
            5 = 完全满足用户需求，信息准确完整，格式规范
            4 = 基本满足需求，小瑕疵可忽略
            3 = 部分满足需求，有明显遗漏或偏差
            2 = 与需求关联弱，信息错误或严重缺失
            1 = 答非所问或空回答
            严格按 JSON 输出（不要多余文字）：{"score": 分数, "comment": "一句话评语，不超过40字"}
            """;

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
    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

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
                // Judge 调用不落日志（避免「谁来评估评估者」的套娃），输出质量由人工抽检兜底
                String json = aiModelSupport.callJson(chatModel, JUDGE_SYSTEM_PROMPT, buildCase(scope, response));
                Judgement judgement = parse(json);
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

    private static String buildCase(String scope, String response) {
        return "场景: " + scope + "\nAI 回答: " + (response != null ? response : "(空)");
    }

    /**
     * 解析 Judge 输出 JSON；解析失败或分数非法返回 null（该条留待下轮，不写脏数据）。
     */
    private Judgement parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Judgement judgement = objectMapper.readValue(json, Judgement.class);
            if (judgement.score() < 1 || judgement.score() > 5) {
                return null;
            }
            return judgement;
        } catch (Exception e) {
            return null;
        }
    }

    private record Judgement(int score, String comment) {}
}
