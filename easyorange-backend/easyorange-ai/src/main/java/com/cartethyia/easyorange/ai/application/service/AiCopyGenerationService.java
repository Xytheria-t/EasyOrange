package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCopyGenerationService {

    private static final String PROMPT_NAME = "ai_copy_generation_system";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

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

        String systemPrompt = promptRegistry.require(PROMPT_NAME);

        String userMessage = String.format(
                """
                <asset_info>
                商品名称：%s
                分类：%s
                成色：%s
                原价：%s
                风格要求：%s
                </asset_info>
                """,
                productName != null ? productName : "",
                categoryName != null ? categoryName : "未知",
                AiModelSupport.formatCondition(conditionLevel),
                originalPrice != null && !originalPrice.isEmpty() ? "¥" + originalPrice : "未知",
                styleDesc);

        var generated = aiModelSupport.callJsonAs(
                chatModel, AiCallScope.COPY, systemPrompt, userMessage, CopyGenerationResult.class);
        if (generated.isEmpty()) {
            log.warn("AI copy generation unavailable for product: {}", productName);
        }
        return generated.orElse(null);
    }
}
