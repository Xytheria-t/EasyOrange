package com.cartethyia.easyorange.product.adapter.inbound.web;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.product.application.query.handler.ProductSearchHandler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.HotKeywordResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchHistoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final ProductSearchHandler searchHandler;

    @GetMapping
    public Result<SearchPageResponse<ProductResponse>> searchProducts(@Valid ProductSearchRequest request) {
        SearchPageResponse<ProductResponse> result = searchHandler.handleSearch(request);
        return Result.success(result);
    }

    @GetMapping("/history")
    public Result<List<SearchHistoryResponse>> getMySearchHistory(
            @RequestParam(defaultValue = "20") @Max(50) Integer limit) {
        List<SearchHistoryResponse> history = searchHandler.getMySearchHistory(limit);
        return Result.success(history);
    }

    @DeleteMapping("/history")
    public Result<Void> clearMySearchHistory() {
        searchHandler.clearMySearchHistory();
        return Result.success();
    }

    @DeleteMapping("/history/{historyId}")
    public Result<Void> deleteSearchHistory(@PathVariable Long historyId) {
        searchHandler.deleteSearchHistory(historyId);
        return Result.success();
    }

    @GetMapping("/hot")
    public Result<List<HotKeywordResponse>> getHotKeywords(
            @RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<HotKeywordResponse> keywords = searchHandler.getHotKeywords(limit);
        return Result.success(keywords);
    }

    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam @Size(max = 100, message = "关键词不能超过 100 个字符") String keyword,
            @RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<String> suggestions = searchHandler.getSearchSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    @PostMapping("/record")
    public Result<Void> recordSearch(@RequestParam @Size(max = 100, message = "关键词不能超过 100 个字符") String keyword) {
        searchHandler.recordSearch(keyword);
        return Result.success();
    }
}
