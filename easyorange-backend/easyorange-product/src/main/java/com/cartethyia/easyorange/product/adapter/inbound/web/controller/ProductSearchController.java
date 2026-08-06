package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.FacetBucketResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.HotKeywordResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchHistoryResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.ProductSearchQueryHandler;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品管理", description = "商品搜索/筛选")
@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
@Validated
public class ProductSearchController {

    private final ProductSearchQueryHandler searchQueryHandler;

    @GetMapping
    public Result<SearchPageResponse<ProductResponse>> searchProducts(@Valid ProductSearchRequest request) {
        var criteria = new ProductSearchCriteria(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getConditionLevel(),
                request.getSortField(),
                null,
                request.getPageNum(),
                request.getPageSize());
        ProductSearchResult result = searchQueryHandler.search(criteria, request.isAiEnhanced());

        var responses = result.page().records().stream()
                .map(ProductSearchController::toProductResponse)
                .toList();
        var facetResponses = result.facets().stream()
                .map(ProductSearchController::toFacetBucketResponse)
                .toList();

        var searchResp = new SearchPageResponse<>(
                responses,
                result.page().total(),
                result.page().current(),
                result.page().size(),
                result.page().pages(),
                facetResponses,
                result.aiEnhancement());
        return Result.success(searchResp);
    }

    @GetMapping("/history")
    public Result<List<SearchHistoryResponse>> getMySearchHistory(
            @RequestParam(defaultValue = "20") @Max(50) Integer limit) {
        List<SearchHistoryReadModel> histories = searchQueryHandler.getMySearchHistory(limit);
        var responses = histories.stream()
                .map(h -> SearchHistoryResponse.builder()
                        .id(h.id())
                        .keyword(h.keyword())
                        .createTime(h.createTime())
                        .build())
                .toList();
        return Result.success(responses);
    }

    @DeleteMapping("/history")
    public Result<Void> clearMySearchHistory() {
        searchQueryHandler.clearMySearchHistory();
        return Result.success();
    }

    @DeleteMapping("/history/{historyId}")
    public Result<Void> deleteSearchHistory(@PathVariable String historyId) {
        searchQueryHandler.deleteSearchHistory(historyId);
        return Result.success();
    }

    @GetMapping("/hot")
    public Result<List<HotKeywordResponse>> getHotKeywords(@RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<HotKeywordReadModel> keywords = searchQueryHandler.getHotKeywords(limit);
        var responses = keywords.stream()
                .map(k -> HotKeywordResponse.builder()
                        .id(k.id())
                        .keyword(k.keyword())
                        .searchCount(k.searchCount())
                        .hotLevel(k.hotLevel())
                        .build())
                .toList();
        return Result.success(responses);
    }

    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam @Size(max = 100, message = "关键词不能超过 100 个字符") String keyword,
            @RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<String> suggestions = searchQueryHandler.getSearchSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    @PostMapping("/record")
    public Result<Void> recordSearch(@RequestParam @Size(max = 100, message = "关键词不能超过 100 个字符") String keyword) {
        searchQueryHandler.recordSearch(keyword);
        return Result.success();
    }

    // ── private DTO converters ──

    private static ProductResponse toProductResponse(ProductReadModel model) {
        return ProductResponse.builder()
                .id(model.id())
                .title(model.title())
                .price(model.price())
                .originalPrice(model.originalPrice())
                .mainImageUrl(model.mainImageUrl())
                .status(model.status())
                .statusDesc(model.statusDesc())
                .condition(model.condition())
                .conditionDesc(model.conditionDesc())
                .location(model.location())
                .createTime(model.createTime())
                .build();
    }

    private static FacetBucketResponse toFacetBucketResponse(FacetBucket fb) {
        return new FacetBucketResponse(fb.key(), fb.count());
    }
}
