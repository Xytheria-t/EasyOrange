package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPricingService {

    private static final String PROMPT_NAME = "ai_pricing_system";

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
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
                商品名称：%s
                描述：%s
                分类：%s
                成色：%s
                原价：%s
                """,
                productName,
                description != null ? description : "无",
                categoryName != null ? categoryName : "未知",
                AiModelSupport.formatCondition(conditionLevel),
                originalPrice != null ? "¥" + originalPrice : "未知");

        try {
            String jsonResponse = aiModelSupport.callJson(chatModel, AiCallScope.PRICING, systemPrompt, userMessage);
            if (jsonResponse == null) {
                return null;
            }
            return objectMapper.readValue(jsonResponse, PricingSuggestion.class);
        } catch (Exception e) {
            log.error("AI pricing failed for product: {}", productName, e);
            return null;
        }
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
