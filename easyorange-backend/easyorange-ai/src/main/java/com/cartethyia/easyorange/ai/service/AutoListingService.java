package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.ai.port.VisionPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoListingService {

    private final VisionPort visionPort;
    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    private static final String VISUAL_PROMPT = """
            请详细描述图片中的商品，包括：
            1. 商品类型和名称
            2. 外观特征（颜色、材质、尺寸等）
            3. 成色和磨损情况
            4. 品牌和型号（如果可见）
            5. 包装和配件情况
            6. 拍摄环境和背景
            """;

    private static final String SYSTEM_PROMPT = """
            你是 EasyOrange — AI 工程化 的智能上架助手。根据视觉分析结果，
            生成完整的商品上架信息。请以 JSON 格式返回，包含以下字段：
            - title: 商品标题（简洁明了，含关键词）
            - description: 商品描述（详细描述商品状况、特点）
            - price: 建议售价（数字，根据成色和市场行情估算）
            - categoryName: 分类名称
            - categoryId: 分类ID（数字，不确定时填 null）
            - conditionLevel: 成色等级（1=全新, 2=九五新, 3=八五新, 4=七成新）
            - location: 所在地（根据图片信息推测）
            - tags: 标签列表（字符串数组，3-5个相关标签）
            - imageDescriptions: 每张图片的简短描述（字符串数组）
            """;

    public AutoListingResult analyzeImages(List<String> imageUrls) {
        try {
            String visualResult = visionPort.analyzeImages(imageUrls, VISUAL_PROMPT);
            if (visualResult == null) {
                log.warn("Vision analysis returned null for {} images", imageUrls.size());
                return null;
            }

            String jsonResponse = llmPort.generateTextWithJson(SYSTEM_PROMPT, visualResult);
            if (jsonResponse == null) {
                log.warn("LLM returned null for auto listing generation");
                return null;
            }

            return objectMapper.readValue(jsonResponse, AutoListingResult.class);
        } catch (Exception e) {
            log.error("Auto listing analysis failed for {} images", imageUrls.size(), e);
            return null;
        }
    }
}