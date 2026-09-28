package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * LLM-as-Judge 评审器 — 用 ChatModel 当评审员给 AI 输出打 1-5 分，唯一消费方是金标准集回归
 * （{@link GoldenSetEvaluator}，CI 的 {@code ai-eval.yml} 触发）；Judge 打分与检索指标共同构成
 * EvalGate 的双门禁。调用不落调用日志（避免「谁来评估评估者」的套娃）。
 * <p>
 * 只保留「对照参考回答评审」一条：金标准集的 chat 用例在 {@code GoldenSetLoader} 加载期就强制要求
 * {@code reference_answer}，无参考回答的通用评分标准没有任何生产可达路径，留着等于一份改不到、
 * 也测不到的旁支评测口径。
 * <p>
 * 评审模型走场景路由（{@code judge} → 当前默认 chatModel）：自评有偏差（同一模型倾向给自己风格的
 * 输出高分），换评审模型只需改 yaml 里该场景的 bean 名，代码零改动 —— 可演进的位，不是遗漏。
 */
@Component
@RequiredArgsConstructor
public class AiJudge {

    /** 评审场景名（{@code easyorange.ai.routing.scenarios} 的键）。 */
    public static final String JUDGE_SCENARIO = "judge";

    /** 评审 system prompt 键（与 {@code prompts/judge.yml} 的 name 同名）。 */
    private static final String JUDGE_PROMPT = "judge_system";

    private final AiModelRouter modelRouter;
    private final PromptRegistryPort promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final ObjectMapper objectMapper;

    /** 对照参考回答评审（回答「AI 质量有没有回归」）—— 1-5 五档量表，正文见 {@code prompts/judge.yml}。 */
    public Optional<Judgement> judgeAgainstReference(String reference, String response) {
        return judgeWith(
                promptRegistry.require(JUDGE_PROMPT),
                "参考回答: " + (reference != null ? reference : "(空)") + "\nAI 回答: "
                        + (response != null ? response : "(空)"));
    }

    private Optional<Judgement> judgeWith(String systemPrompt, String caseText) {
        try {
            String json = aiModelSupport.callJson(modelRouter.choose(JUDGE_SCENARIO), systemPrompt, caseText);
            return parse(json);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 解析 Judge 输出 JSON；解析失败或分数非法返回 empty（调用方跳过该条，不写脏数据）。 */
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
