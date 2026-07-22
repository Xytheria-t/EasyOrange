package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCopyGenerationService {

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    public CopyGenerationResult generateCopy(
            String productName,
            String categoryName,
            String conditionLevel,
            String originalPrice,
            String style
    ) {
        String styleDesc = switch (style != null ? style : "standard") {
            case "detailed" -> "详细详尽型：详细描述商品的品牌、型号、规格、材质、使用感受等所有细节";
            case "concise" -> "简洁明了型：用简短的文字突出商品核心卖点和亮点";
            case "emotional" -> "情感共鸣型：用温暖感性的语言讲述商品故事，激发认领方情感共鸣";
            default -> "标准推荐型：平衡描述商品的基本信息和卖点，适合大多数商品";
        };

        String systemPrompt = """
                你是 EasyOrange — AI 工程化 的智能文案生成助手。根据资产信息生成吸引人的资产标题和描述。
                请以 JSON 格式返回，包含字段：
                - title: 商品标题（简洁有吸引力，含关键词，15-30字）
                - description: 商品描述（详细描述商品状况、特点、卖点，200-500字）
                - style: 使用的文案风格
                """;

        String userMessage = String.format("""
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
                styleDesc
        );

        try {
            String jsonResponse = llmPort.generateTextWithJson(systemPrompt, userMessage);
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