package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    public AiReviewResult reviewProduct(
            String productName,
            String description,
            String categoryName,
            Integer conditionLevel,
            String price,
            String sellerName,
            List<String> imageUrls
    ) {
        String systemPrompt = """
                你是 EasyOrange AI 智能托管平台的商品审核助手。
                根据商品信息，判断该商品是否符合平台审核标准。
                审核标准：
                1. 商品信息是否完整准确
                2. 商品描述是否与标题一致
                3. 价格是否合理（是否存在明显异常高价或低价）
                4. 是否存在违规内容（违禁品、虚假信息等）
                5. 商品图片与描述是否匹配
                请以 JSON 格式返回，包含字段：
                - suggestedAction: 建议操作（true=通过, false=拒绝）
                - suggestedActionDesc: "通过" 或 "拒绝"
                - confidenceScore: 置信度（1-100的整数）
                - riskFlags: 风险标记数组（如 ["价格异常","描述不清"] ，没有风险则返回空数组）
                - reasoning: 审核理由（50字内）
                """;

        String userMessage = String.format("""
                商品名称：%s
                描述：%s
                分类：%s
                成色：%s
                价格：%s
                卖家：%s
                图片数量：%d张
                """,
                productName,
                description != null ? description : "无",
                categoryName != null ? categoryName : "未知",
                formatCondition(conditionLevel),
                price,
                sellerName,
                imageUrls != null ? imageUrls.size() : 0
        );

        try {
            String jsonResponse = llmPort.generateTextWithJson(systemPrompt, userMessage);
            if (jsonResponse == null) {
                return new AiReviewResult(true, "通过", 50, List.of(), "AI 无法分析，默认通过");
            }
            return objectMapper.readValue(jsonResponse, AiReviewResult.class);
        } catch (Exception e) {
            log.error("AI review failed for product: {}", productName, e);
            return new AiReviewResult(true, "通过", 50, List.of(), "AI 分析异常，默认通过");
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