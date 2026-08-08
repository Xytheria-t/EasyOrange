package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.dto.QaRequest;
import com.cartethyia.easyorange.ai.dto.QaResponse;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQaService {

    private static final String PROMPT_NAME = "ai_qa_system";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

    @TokenBudget(scenario = "qa", maxTokensPerCall = 1000, dailyTokenLimit = 200_000)
    public QaResponse answerQuestion(QaRequest request) {
        String systemPrompt = loadSystemPrompt();
        String userMessage = buildUserMessage(request);
        log.debug("Answering question for productId={}, question={}", request.productId(), request.question());

        try {
            String answer = aiModelSupport.callText(chatModel, AiCallScope.QA, systemPrompt, userMessage);

            if (answer == null || answer.isBlank()) {
                log.warn("AI returned empty answer for productId={}", request.productId());
                return new QaResponse("AI服务暂时不可用", false);
            }

            return new QaResponse(answer, true);
        } catch (Exception e) {
            log.error("Failed to generate answer for productId={}", request.productId(), e);
            return new QaResponse("AI服务暂时不可用", false);
        }
    }

    private String loadSystemPrompt() {
        return promptRegistry
                .getLatest(PROMPT_NAME)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + PROMPT_NAME));
    }

    private String buildUserMessage(QaRequest request) {
        return String.format(
                """
                商品信息：
                - 名称：%s
                - 描述：%s
                - 分类：%s
                - 价格：%s
                - 成色：%s
                - 资产方：%s
                - 资产方信誉等级：%s

                用户提问：%s
                """,
                request.productName(),
                request.productDescription(),
                request.categoryName(),
                request.price(),
                request.conditionLevel(),
                request.sellerName(),
                request.sellerCreditLevel(),
                request.question());
    }
}
