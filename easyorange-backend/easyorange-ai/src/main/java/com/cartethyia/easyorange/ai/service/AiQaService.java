package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.QaRequest;
import com.cartethyia.easyorange.ai.dto.QaResponse;
import com.cartethyia.easyorange.ai.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQaService {

    private final LlmPort llmPort;

    private static final String SYSTEM_PROMPT = """
            你是 EasyOrange — AI 工程化 的智能客服助手。
            请基于以下商品信息回答用户的问题。
            只回答与商品相关的问题，如果问题与商品无关，请礼貌地告知用户只能回答商品相关问题。
            回答控制在50字以内，简洁准确。
            """;

    public QaResponse answerQuestion(QaRequest request) {
        String userMessage = buildUserMessage(request);
        log.debug("Answering question for productId={}, question={}", request.productId(), request.question());

        try {
            String answer = llmPort.generateText(SYSTEM_PROMPT, userMessage);

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

    private String buildUserMessage(QaRequest request) {
        return String.format("""
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
                request.question()
        );
    }
}