package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPricingService {

    private static final String PROMPT_NAME = "ai_pricing_system";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

    @TokenBudget(scenario = "pricing", maxTokensPerCall = 2000, dailyTokenLimit = 500_000)
    public PricingSuggestion suggestPrice(
            String productName,
            String description,
            String categoryName,
            String conditionLevel,
            BigDecimal originalPrice) {
        String systemPrompt = loadSystemPrompt();

        String userMessage = String.format(
                """
                <asset_info>
                商品名称：%s
                描述：%s
                分类：%s
                成色：%s
                原价：%s
                </asset_info>
                """,
                productName,
                description != null ? description : "无",
                categoryName != null ? categoryName : "未知",
                AiModelSupport.formatCondition(conditionLevel),
                originalPrice != null ? "¥" + originalPrice : "未知");

        var suggestion = aiModelSupport.callJsonAs(
                chatModel, AiCallScope.PRICING, systemPrompt, userMessage, PricingSuggestion.class);
        if (suggestion.isEmpty()) {
            log.warn("AI pricing unavailable for product: {}", productName);
        }
        return suggestion.orElse(null);
    }

    /**
     * 从 PromptRegistry 加载系统提示词（版本化、可热更新）。
     * YAML 缺失时 fail-fast，避免静默使用错误 prompt。
     */
    private String loadSystemPrompt() {
        return promptRegistry
                .getLatest(PROMPT_NAME)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + PROMPT_NAME));
    }
}
