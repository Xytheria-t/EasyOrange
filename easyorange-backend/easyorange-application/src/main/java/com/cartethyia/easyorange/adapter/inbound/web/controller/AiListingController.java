package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.dto.PricingRequest;
import com.cartethyia.easyorange.ai.application.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.application.service.AiPricingService;
import com.cartethyia.easyorange.ai.application.service.AutoListingService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 上架辅助端点 — 智能估值 / 自动上架（Vision）。
 * <p>
 * 商品审核不在这里：审核建议由管理端按需触发（{@code GET /api/admin/products/{id}/ai-review}），
 * 卖家侧没有入口 —— 原先这里还开着 {@code POST /api/ai/review}，但全仓无调用方，
 * 一个公开的、可触发模型调用的重复入口只增加被刷成本与被追问的「为什么有两个审核入口」。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 上架辅助：智能估值 / 自动上架")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiListingController {

    private final AiPricingService pricingService;
    private final AutoListingService autoListingService;

    @PostMapping("/pricing")
    public Result<PricingSuggestion> suggestPrice(@Valid @RequestBody PricingRequest request) {
        return Result.success(pricingService.suggestPrice(
                request.productName(),
                request.description(),
                request.categoryName(),
                request.conditionLevel(),
                request.originalPrice()));
    }

    @PostMapping("/auto-listing")
    public Result<AutoListingResult> autoListing(@RequestBody List<String> imageUrls) {
        return Result.success(autoListingService.analyzeImages(imageUrls));
    }
}
