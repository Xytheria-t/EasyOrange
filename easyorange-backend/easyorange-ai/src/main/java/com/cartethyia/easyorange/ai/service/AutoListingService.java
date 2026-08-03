package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoListingService {

    private static final String VISUAL_PROMPT_NAME = "auto_listing_visual";
    private static final String SYSTEM_PROMPT_NAME = "auto_listing_system";

    private final ChatModel chatModel;
    @Qualifier("visionChatModel")
    private final ChatModel visionChatModel;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;

    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(List<String> imageUrls) {
        try {
            String visualPrompt = loadPrompt(VISUAL_PROMPT_NAME);
            String systemPrompt = loadPrompt(SYSTEM_PROMPT_NAME);

            String visualResult = AiModelSupport.analyzeImages(visionChatModel, imageUrls, visualPrompt);
            if (visualResult == null) {
                log.warn("Vision analysis returned null for {} images", imageUrls.size());
                return null;
            }

            String jsonResponse = AiModelSupport.callJson(chatModel, systemPrompt, visualResult);
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
        return promptRegistry.getLatest(name)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException(
                        "Prompt template not found: " + name));
    }
}
