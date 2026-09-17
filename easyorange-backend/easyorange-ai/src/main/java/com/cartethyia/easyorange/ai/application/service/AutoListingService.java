package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoListingService {

    private static final String VISUAL_PROMPT_NAME = "auto_listing_visual";
    private static final String SYSTEM_PROMPT_NAME = "auto_listing_system";

    private final ChatModel chatModel;
    private final AiModelRouter modelRouter;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(List<String> imageUrls) {
        try {
            String visualPrompt = loadPrompt(VISUAL_PROMPT_NAME);
            String systemPrompt = loadPrompt(SYSTEM_PROMPT_NAME);

            // 视觉分析走场景路由（vision → visionChatModel），与文本生成解耦、可独立换模型；
            // 带 scope 以便视觉模型的 token 用量计入 auto_listing 场景预算
            String visualResult = aiModelSupport.analyzeImages(
                    modelRouter.choose("vision"), AiCallScope.AUTO_LISTING, imageUrls, visualPrompt);
            if (visualResult == null) {
                log.warn("Vision analysis returned null for {} images", imageUrls.size());
                return null;
            }

            String jsonResponse =
                    aiModelSupport.callJson(chatModel, AiCallScope.AUTO_LISTING, systemPrompt, visualResult);
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

    private String loadPrompt(String name) {
        return promptRegistry
                .getLatest(name)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + name));
    }
}
