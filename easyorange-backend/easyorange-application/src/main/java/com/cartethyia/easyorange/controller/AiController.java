package com.cartethyia.easyorange.controller;

import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
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
import com.cartethyia.easyorange.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

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
    public Result<PricingSuggestion> suggestPrice(
            @RequestParam String productName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Integer conditionLevel,
            @RequestParam(required = false) BigDecimal originalPrice
    ) {
        var suggestion = pricingService.suggestPrice(
                productName, description, categoryName, conditionLevel, originalPrice);
        return Result.success(suggestion);
    }

    @PostMapping("/auto-listing")
    public Result<AutoListingResult> autoListing(@RequestBody List<String> imageUrls) {
        var result = autoListingService.analyzeImages(imageUrls);
        return Result.success(result);
    }

    @PostMapping("/review")
    public Result<AiReviewResult> reviewProduct(
            @RequestParam String productName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Integer conditionLevel,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String sellerName,
            @RequestBody(required = false) List<String> imageUrls
    ) {
        var result = reviewService.reviewProduct(
                productName, description, categoryName, conditionLevel,
                price, sellerName, imageUrls);
        return Result.success(result);
    }

    @GetMapping("/semantic-search")
    public Result<SemanticSearchResult> semanticSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        var result = semanticSearchService.search(keyword, pageNum, pageSize);
        return Result.success(result);
    }

    @PostMapping("/qa")
    public Result<QaResponse> answerQuestion(@RequestBody QaRequest request) {
        var response = qaService.answerQuestion(request);
        return Result.success(response);
    }

    @PostMapping("/generate-copy")
    public Result<CopyGenerationResult> generateCopy(
            @RequestParam String productName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Integer conditionLevel,
            @RequestParam(required = false) String originalPrice,
            @RequestParam(defaultValue = "standard") String style
    ) {
        var result = copyGenerationService.generateCopy(
                productName, categoryName, conditionLevel, originalPrice, style);
        return Result.success(result);
    }
}