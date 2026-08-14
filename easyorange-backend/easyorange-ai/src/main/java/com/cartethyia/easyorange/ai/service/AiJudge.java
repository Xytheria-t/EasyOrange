package com.cartethyia.easyorange.ai.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * LLM-as-Judge 评审器 — 用 ChatModel 当评审员，按四维标准给 AI 输出打 1-5 分。
 * <p>
 * 供两处复用：{@link com.cartethyia.easyorange.ai.adapter.inbound.job.AiEvalScheduler}
 * 对 eo_ai_call_log 未评审记录打分；金标准集回归（GoldenSetEvaluator）对测试用例打分。
 * Judge 调用不落调用日志（避免「谁来评估评估者」的套娃）。
 */
@Component
@RequiredArgsConstructor
public class AiJudge {

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

    private static final String REFERENCE_JUDGE_SYSTEM_PROMPT = """
            你是 AI 输出质量评审员（Judge）。请对照「参考回答」评审「AI 回答」。
            评分标准（1-5 分）：
            5 = 与参考回答语义一致，要点齐全且无错误信息
            4 = 基本一致，遗漏 1 个次要要点
            3 = 部分一致，有明显遗漏或偏差
            2 = 与参考回答关联弱，关键要点错误或缺失
            1 = 答非所问或空回答
            严格按 JSON 输出（不要多余文字）：{"score": 分数, "comment": "一句话评语，不超过40字"}
            """;

    private final ChatModel chatModel;
    private final AiModelSupport aiModelSupport;
    private final ObjectMapper objectMapper;

    /**
     * 无参考回答的通用评审（四维标准）。
     */
    public Optional<Judgement> judge(String scope, String response) {
        return judgeWith(JUDGE_SYSTEM_PROMPT, "场景: " + scope + "\nAI 回答: " + (response != null ? response : "(空)"));
    }

    /**
     * 对照参考回答评审（金标准集回归用，回答「AI 质量有没有回归」）。
     */
    public Optional<Judgement> judgeAgainstReference(String reference, String response) {
        return judgeWith(
                REFERENCE_JUDGE_SYSTEM_PROMPT,
                "参考回答: " + (reference != null ? reference : "(空)") + "\nAI 回答: "
                        + (response != null ? response : "(空)"));
    }

    private Optional<Judgement> judgeWith(String systemPrompt, String caseText) {
        try {
            String json = aiModelSupport.callJson(chatModel, systemPrompt, caseText);
            return parse(json);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 解析 Judge 输出 JSON；解析失败或分数非法返回 empty（调用方跳过该条，不写脏数据）。
     */
    private Optional<Judgement> parse(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            Judgement judgement = objectMapper.readValue(json, Judgement.class);
            if (judgement.score() < 1 || judgement.score() > 5) {
                return Optional.empty();
            }
            return Optional.of(judgement);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record Judgement(int score, String comment) {}
}
