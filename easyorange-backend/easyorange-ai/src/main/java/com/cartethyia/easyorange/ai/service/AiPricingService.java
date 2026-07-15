package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.port.LlmPort;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPricingService {

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    public PricingSuggestion suggestPrice(
            String productName,
            String description,
            String categoryName,
            Integer conditionLevel,
            BigDecimal originalPrice
    ) {
        String systemPrompt = """
                你是 EasyOrange — AI 工程化 的智能估值助手。根据资产信息，
                分析市场价格并给出建议售价。
                请以 JSON 格式返回，包含字段：
                - suggestedPrice: 建议售价（数字）
                - minPrice: 最低建议价（数字）
                - maxPrice: 最高建议价（数字）
                - reasoning: 定价理由（字符串，50字内）
                - marketContext: 市场行情（字符串，30字内）
                """;

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

    private String formatCondition(Integer conditionLevel) {
        if (conditionLevel == null) return "未知";
        return switch (conditionLevel) {
            case 1 -> "全新";
            case 2 -> "九五新";
            case 3 -> "八五新";
            case 4 -> "七成新";
            default -> "未知";
        };
    }
}