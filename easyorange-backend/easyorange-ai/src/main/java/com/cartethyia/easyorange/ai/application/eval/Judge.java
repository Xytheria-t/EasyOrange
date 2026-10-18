package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.application.support.ModelCaller;
import com.cartethyia.easyorange.ai.application.support.ModelRouter;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * LLM-as-Judge 评审器 — 用 ChatModel 给 AI 输出打 1-5 分，唯一消费方是金标准集回归
 * （{@link GoldenSetEvaluator}，CI 的 {@code ai-eval.yml} 触发），与检索指标共同构成 EvalGate 双门禁；
 * 调用不落调用日志（避免「谁来评估评估者」的套娃）。评审模型走 {@code judge} 路由角色：自评有偏差
 * （同一模型倾向给自己风格的输出高分），角色名在 {@link ModelRouter} 单点定义，换评审模型只改 yaml。
 * <p>
 * 只保留「对照参考回答评审」一条：chat 用例在 {@code GoldenSetLoader} 加载期就强制要求
 * {@code reference_answer}，无参考回答的通用评分标准没有生产可达路径，留着只是一份改不到、也测不到的旁支口径。
 */
@Component
@RequiredArgsConstructor
public class Judge {

    /** 评审 system prompt 键（与 {@code prompts/judge.yml} 的 name 同名）。 */
    private static final String JUDGE_PROMPT = "judge_system";

    private final PromptRegistryPort promptRegistry;
    private final ModelCaller modelCaller;
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
            String json = modelCaller.callJson(ModelRouter.SCENARIO_JUDGE, systemPrompt, caseText);
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
