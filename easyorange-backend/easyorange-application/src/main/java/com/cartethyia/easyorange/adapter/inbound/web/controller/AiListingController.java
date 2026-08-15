package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.AiReviewRequest;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.dto.PricingRequest;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.service.AiPricingService;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.ai.service.AutoListingService;
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
 * AI 上架辅助端点 — 智能估值 / 自动上架（Vision）/ 商品审核。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 上架辅助：智能估值 / 自动上架 / 商品审核")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiListingController {

    private final AiPricingService pricingService;
    private final AutoListingService autoListingService;
    private final AiReviewService reviewService;

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

    @PostMapping("/review")
    public Result<AiReviewResult> reviewProduct(@Valid @RequestBody AiReviewRequest request) {
        return Result.success(reviewService.reviewProduct(
                request.productName(),
                request.description(),
                request.categoryName(),
                request.conditionLevel(),
                request.price(),
                request.sellerName(),
                request.imageUrls()));
    }
}
