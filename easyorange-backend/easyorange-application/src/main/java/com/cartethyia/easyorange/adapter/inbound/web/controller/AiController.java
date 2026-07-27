package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.AiReviewRequest;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.dto.CopyGenerationRequest;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.dto.PricingRequest;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.dto.QaRequest;
import com.cartethyia.easyorange.ai.dto.QaResponse;
import com.cartethyia.easyorange.ai.dto.SemanticSearchResult;
import com.cartethyia.easyorange.ai.service.AiCopyGenerationService;
import com.cartethyia.easyorange.ai.service.AiPricingService;
import com.cartethyia.easyorange.ai.service.AiQaService;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.ai.service.AutoListingService;
import com.cartethyia.easyorange.ai.service.SemanticSearchService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 智能估值/审核/文案/客服/语义搜索/自动上架")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiPricingService pricingService;
    private final AutoListingService autoListingService;
    private final AiReviewService reviewService;
    private final SemanticSearchService semanticSearchService;
    private final AiQaService qaService;
    private final AiCopyGenerationService copyGenerationService;

    @PostMapping("/pricing")
    public Result<PricingSuggestion> suggestPrice(@RequestBody PricingRequest request) {
        return Result.success(pricingService.suggestPrice(
                request.productName(), request.description(), request.categoryName(),
                request.conditionLevel(), request.originalPrice()));
    }

    @PostMapping("/auto-listing")
    public Result<AutoListingResult> autoListing(@RequestBody List<String> imageUrls) {
        return Result.success(autoListingService.analyzeImages(imageUrls));
    }

    @PostMapping("/review")
    public Result<AiReviewResult> reviewProduct(@RequestBody AiReviewRequest request) {
        return Result.success(reviewService.reviewProduct(
                request.productName(), request.description(), request.categoryName(),
                request.conditionLevel(), request.price(), request.sellerName(),
                request.imageUrls()));
    }

    @GetMapping("/semantic-search")
    public Result<SemanticSearchResult> semanticSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.success(semanticSearchService.search(keyword, pageNum, pageSize));
    }

    @PostMapping("/qa")
    public Result<QaResponse> answerQuestion(@RequestBody QaRequest request) {
        return Result.success(qaService.answerQuestion(request));
    }

    @PostMapping("/generate-copy")
    public Result<CopyGenerationResult> generateCopy(@RequestBody CopyGenerationRequest request) {
        return Result.success(copyGenerationService.generateCopy(
                request.productName(), request.categoryName(), request.conditionLevel(),
                request.originalPrice(), request.style() != null ? request.style() : "standard"));
    }
}