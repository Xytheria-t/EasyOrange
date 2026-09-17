package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoListingService {

    private static final String VISUAL_PROMPT_NAME = "auto_listing_visual";
    private static final String SYSTEM_PROMPT_NAME = "auto_listing_system";

    private final ChatModel chatModel;
    private final AiModelRouter modelRouter;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;

    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(List<String> imageUrls) {
        // 模板缺失是部署期配置错误，不进降级（与其余 AI 服务一致）：静默返回 null
        // 会把「prompt 没打进包」伪装成「AI 暂时不可用」
        String visualPrompt = promptRegistry.require(VISUAL_PROMPT_NAME);
        String systemPrompt = promptRegistry.require(SYSTEM_PROMPT_NAME);

        try {
            // 视觉分析走场景路由（vision → visionChatModel），与文本生成解耦、可独立换模型；
            // 带 scope 以便视觉模型的 token 用量计入 auto_listing 场景预算
            String visualResult = aiModelSupport.analyzeImages(
                    modelRouter.choose("vision"), AiCallScope.AUTO_LISTING, imageUrls, visualPrompt);
            if (visualResult == null || visualResult.isBlank()) {
                log.warn("Vision analysis returned nothing for {} images", imageUrls.size());
                return null;
            }

            var listing = aiModelSupport.callJsonAs(
                    chatModel, AiCallScope.AUTO_LISTING, systemPrompt, visualResult, AutoListingResult.class);
            if (listing.isEmpty()) {
                log.warn("AI auto listing unavailable for {} images", imageUrls.size());
            }
            return listing.orElse(null);
        } catch (Exception e) {
            log.error("Auto listing analysis failed for {} images", imageUrls.size(), e);
            return null;
        }
    }
}
