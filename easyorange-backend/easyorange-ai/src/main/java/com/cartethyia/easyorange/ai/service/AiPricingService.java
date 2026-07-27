package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPricingService {

    private static final String PROMPT_NAME = "ai_pricing_system";

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;

    @TokenBudget(scenario = "pricing", maxTokensPerCall = 2000, dailyTokenLimit = 500_000)
    public PricingSuggestion suggestPrice(
            String productName,
            String description,
            String categoryName,
            String conditionLevel,
            BigDecimal originalPrice
    ) {
        String systemPrompt = loadSystemPrompt();

        String userMessage = String.format("""
                商品名称：%s
                描述：%s
                分类：%s
                成色：%s
                原价：%s
                """,
                productName,
                description != null ? description : "无",
                categoryName != null ? categoryName : "未知",
                formatCondition(conditionLevel),
                originalPrice != null ? "¥" + originalPrice : "未知"
        );

        try {
            String jsonResponse = llmPort.generateTextWithJson(systemPrompt, userMessage);
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
        return promptRegistry.getLatest(PROMPT_NAME)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException(
                        "Prompt template not found: " + PROMPT_NAME));
    }

    private String formatCondition(String conditionLevel) {
        if (conditionLevel == null) return "未知";
        return switch (conditionLevel) {
            case "1" -> "全新";
            case "2" -> "九五新";
            case "3" -> "八五新";
            case "4" -> "七成新";
            default -> "未知";
        };
    }
}
