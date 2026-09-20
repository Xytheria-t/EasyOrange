package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.constant.AiResultCode;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 拍照识别（发布助手）— 一次多模态调用产出上架表单要的全部字段（标题 / 描述 / 建议价 / 类目 / 成色 / 所在地）。
 * <p>
 * 图片与「要哪些字段」在同一次请求里交给视觉模型，不再有第二次文本调用：先前那种「视觉模型写自由文本、
 * 文本模型再转 JSON」的两段式，第二次调用看不到图片，只是做格式转换 —— 白付一次调用的钱与延迟。
 * <p>
 * 失败一律显式报错（{@link AiResultCode#AI_UNAVAILABLE}）：这里没有「部分可用」的结果，
 * 静默返回 null 只会让用户点了按钮、等一会儿、零反馈。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoListingService {

    private static final String PROMPT_NAME = "auto_listing";

    private final AiModelRouter modelRouter;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final CategoryCatalogPort categoryCatalogPort;

    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(List<String> imageUrls) {
        // 模板缺失是部署期配置错误，不进降级：静默报「AI 暂时不可用」会把「prompt 没打进包」藏起来
        String prompt = promptRegistry.require(PROMPT_NAME);

        var listing = callVision(imageUrls, prompt);
        if (listing.isEmpty()) {
            log.warn("Auto listing analysis returned nothing for {} images", imageUrls.size());
            throw BusinessException.of(AiResultCode.AI_UNAVAILABLE);
        }
        return listing.get();
    }

    /** 供应商异常返回空 Optional，与「模型输出不可解析」收敛成同一个用户可见结果（识别失败）。 */
    private Optional<AutoListingResult> callVision(List<String> imageUrls, String prompt) {
        try {
            // 视觉模型走场景路由（vision → visionChatModel），与文本生成解耦、可独立换模型；
            // 带 scope 以便视觉模型的 token 用量计入 auto_listing 场景预算
            return aiModelSupport.callJsonAsWithImages(
                    modelRouter.choose("vision"),
                    AiCallScope.AUTO_LISTING,
                    prompt,
                    userMessage(),
                    imageUrls,
                    AutoListingResult.class);
        } catch (Exception e) {
            log.error("Auto listing analysis failed for {} images", imageUrls.size(), e);
            return Optional.empty();
        }
    }

    /**
     * 类目清单现查现用 —— 一次小 SELECT，相对多模态调用耗时可以忽略，清单改了下次识别就生效，
     * 省掉一层缓存失效策略。
     */
    private String userMessage() {
        return "可用分类清单：" + String.join("、", categoryCatalogPort.listAvailableCategoryNames()) + "\n请识别图片中的商品并返回上架信息。";
    }
}
