package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private static final String PROMPT_NAME = "ai_review_system";

    /**
     * AI 不可用时的风险标记 —— 管理端据此识别「这条建议无效，需人工审核」。
     * <p>
     * 与前端 {@code AiReviewSuggestion} 的约定：带此标记时不渲染「采纳 AI 建议」按钮。
     */
    public static final String FLAG_UNAVAILABLE = "AI_UNAVAILABLE";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

    @TokenBudget(scenario = "review", maxTokensPerCall = 2000, dailyTokenLimit = 500_000)
    public AiReviewResult reviewProduct(
            String productName,
            String description,
            String categoryName,
            String conditionLevel,
            String price,
            String sellerName,
            List<String> imageUrls) {
        // 模板缺失是部署期配置错误，不进降级：静默降级会把「配置错」伪装成「AI 看不了」
        String systemPrompt = promptRegistry.require(PROMPT_NAME);

        String userMessage = String.format(
                """
                <asset_info>
                商品名称：%s
                描述：%s
                分类：%s
                成色：%s
                价格：%s
                资产方：%s
                图片数量：%d张
                </asset_info>
                """,
                productName,
                description != null ? description : "无",
                categoryName != null ? categoryName : "未知",
                AiModelSupport.formatCondition(conditionLevel),
                price,
                sellerName,
                imageUrls != null ? imageUrls.size() : 0);

        // 模型故障 / 返回空 / JSON 不合 schema 三种情况在 callJsonAs 里统一成 empty，
        // 对审核建议而言降级方向也只有一种：交人工（原因差异只影响排查，见 AiModelSupport 的 warn 日志）
        return aiModelSupport
                .callJsonAs(chatModel, AiCallScope.REVIEW, systemPrompt, userMessage, AiReviewResult.class)
                .orElseGet(() -> {
                    log.warn("AI review unavailable for product: {}", productName);
                    return unavailable("AI 无法分析，请人工审核");
                });
    }

    /**
     * AI 不可用时的降级结果。
     * <p>
     * <b>降级方向必须是「不确定」而不是「通过」</b>：{@code suggestedAction} 直接驱动管理端的
     * 「采纳 AI 建议」按钮（true → 一键通过）。AI 挂掉时给「通过」，等于把「AI 没看成」
     * 变成「平台放行」；而给「拒绝」又会误导成误杀。这里返回 0 置信度 + {@link #FLAG_UNAVAILABLE}，
     * 语义是「本次 AI 建议无效」，由人工接管 —— 与限流 fail-open（不影响用户）不同，
     * 审核建议的降级方向要 fail-safe。
     * <p>
     * 调用方（{@code AdminProductAuditAdapter}）兜底时复用本工厂，避免降级口径写成两份。
     */
    public static AiReviewResult unavailable(String reasoning) {
        return new AiReviewResult(false, "无法判定", 0, List.of(FLAG_UNAVAILABLE), reasoning);
    }
}
