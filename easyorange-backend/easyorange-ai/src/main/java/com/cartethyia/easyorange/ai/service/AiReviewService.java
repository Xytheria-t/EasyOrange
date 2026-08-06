package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private static final String PROMPT_NAME = "ai_review_system";

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;

    @TokenBudget(scenario = "review", maxTokensPerCall = 2000, dailyTokenLimit = 500_000)
    public AiReviewResult reviewProduct(
            String productName,
            String description,
            String categoryName,
            String conditionLevel,
            String price,
            String sellerName,
            List<String> imageUrls) {
        String systemPrompt = loadSystemPrompt();

        String userMessage = String.format(
                """
                商品名称：%s
                描述：%s
                分类：%s
                成色：%s
                价格：%s
                资产方：%s
                图片数量：%d张
                """,
                productName,
                description != null ? description : "无",
                categoryName != null ? categoryName : "未知",
                AiModelSupport.formatCondition(conditionLevel),
                price,
                sellerName,
                imageUrls != null ? imageUrls.size() : 0);

        try {
            String jsonResponse = AiModelSupport.callJson(chatModel, systemPrompt, userMessage);
            if (jsonResponse == null) {
                return new AiReviewResult(true, "通过", 50, List.of(), "AI 无法分析，默认通过");
            }
            return objectMapper.readValue(jsonResponse, AiReviewResult.class);
        } catch (Exception e) {
            log.error("AI review failed for product: {}", productName, e);
            return new AiReviewResult(true, "通过", 50, List.of(), "AI 分析异常，默认通过");
        }
    }

    private String loadSystemPrompt() {
        return promptRegistry
                .getLatest(PROMPT_NAME)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + PROMPT_NAME));
    }
}
