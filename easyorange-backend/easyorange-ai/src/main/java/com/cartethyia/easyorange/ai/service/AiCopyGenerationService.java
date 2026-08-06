package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCopyGenerationService {

    private static final String PROMPT_NAME = "ai_copy_generation_system";

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;

    @TokenBudget(scenario = "copy", maxTokensPerCall = 2500, dailyTokenLimit = 500_000)
    public CopyGenerationResult generateCopy(
            String productName, String categoryName, String conditionLevel, String originalPrice, String style) {
        String styleDesc =
                switch (style != null ? style : "standard") {
                    case "detailed" -> "详细详尽型：详细描述商品的品牌、型号、规格、材质、使用感受等所有细节";
                    case "concise" -> "简洁明了型：用简短的文字突出商品核心卖点和亮点";
                    case "emotional" -> "情感共鸣型：用温暖感性的语言讲述商品故事，激发认领方情感共鸣";
                    default -> "标准推荐型：平衡描述商品的基本信息和卖点，适合大多数商品";
                };

        String systemPrompt = loadSystemPrompt();

        String userMessage = String.format(
                """
                商品名称：%s
                分类：%s
                成色：%s
                原价：%s
                风格要求：%s
                """,
                productName != null ? productName : "",
                categoryName != null ? categoryName : "未知",
                formatCondition(conditionLevel),
                originalPrice != null && !originalPrice.isEmpty() ? "¥" + originalPrice : "未知",
                styleDesc);

        try {
            String jsonResponse = AiModelSupport.callJson(chatModel, systemPrompt, userMessage);
            if (jsonResponse == null) {
                log.warn("LLM returned null for copy generation");
                return null;
            }
            return objectMapper.readValue(jsonResponse, CopyGenerationResult.class);
        } catch (Exception e) {
            log.error("AI copy generation failed for product: {}", productName, e);
            return null;
        }
    }

    private String loadSystemPrompt() {
        return promptRegistry
                .getLatest(PROMPT_NAME)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + PROMPT_NAME));
    }

    private String formatCondition(String conditionLevel) {
        if (conditionLevel == null) return "未知";
        return switch (conditionLevel) {
            case "1" -> "全新（未拆封）";
            case "2" -> "九五新（几乎无使用痕迹）";
            case "3" -> "八五新（正常使用痕迹）";
            case "4" -> "七成新（明显使用痕迹）";
            default -> "未知";
        };
    }
}
