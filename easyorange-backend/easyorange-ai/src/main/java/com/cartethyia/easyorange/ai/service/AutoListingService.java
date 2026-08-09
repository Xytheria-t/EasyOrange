package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AutoListingService {

    private static final String VISUAL_PROMPT_NAME = "auto_listing_visual";
    private static final String SYSTEM_PROMPT_NAME = "auto_listing_system";

    private final ChatModel chatModel;
    private final ChatModel visionChatModel;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

    // Lombok 构造器不会把 @Qualifier 复制到参数上，故手写显式构造器以保留 "visionChatModel" 限定
    public AutoListingService(
            ChatModel chatModel,
            @Qualifier("visionChatModel") ChatModel visionChatModel,
            ObjectMapper objectMapper,
            PromptRegistry promptRegistry,
            AiModelSupport aiModelSupport) {
        this.chatModel = chatModel;
        this.visionChatModel = visionChatModel;
        this.objectMapper = objectMapper;
        this.promptRegistry = promptRegistry;
        this.aiModelSupport = aiModelSupport;
    }

    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(List<String> imageUrls) {
        try {
            String visualPrompt = loadPrompt(VISUAL_PROMPT_NAME);
            String systemPrompt = loadPrompt(SYSTEM_PROMPT_NAME);

            String visualResult = aiModelSupport.analyzeImages(visionChatModel, imageUrls, visualPrompt);
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
