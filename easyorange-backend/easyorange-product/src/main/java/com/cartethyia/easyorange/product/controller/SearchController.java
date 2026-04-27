package com.cartethyia.easyorange.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.product.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.dto.vo.HotKeywordVO;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.dto.vo.SearchHistoryVO;
import com.cartethyia.easyorange.product.service.SearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索商品（支持关键词全文搜索 + 分类/价格/成色过滤）
     */
    @GetMapping
    public Result<Page<ProductVO>> searchProducts(@Valid ProductSearchRequest request) {
        Page<ProductVO> page = searchService.searchProducts(request);
        return Result.success(page);
    }

    /**
     * 获取我的搜索历史
     */
    @GetMapping("/history")
    public Result<List<SearchHistoryVO>> getMySearchHistory(
            @RequestParam(defaultValue = "20") @Max(50) Integer limit) {
        List<SearchHistoryVO> history = searchService.getMySearchHistory(limit);
        return Result.success(history);
    }

    /**
     * 清空我的搜索历史
     */
    @DeleteMapping("/history")
    public Result<Void> clearMySearchHistory() {
        searchService.clearMySearchHistory();
        return Result.success();
    }

    /**
     * 删除单条搜索历史
     */
    @DeleteMapping("/history/{historyId}")
    public Result<Void> deleteSearchHistory(@PathVariable Long historyId) {
        searchService.deleteSearchHistory(historyId);
        return Result.success();
    }

    /**
     * 获取热搜词列表
     */
    @GetMapping("/hot")
    public Result<List<HotKeywordVO>> getHotKeywords(
            @RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<HotKeywordVO> keywords = searchService.getHotKeywords(limit);
        return Result.success(keywords);
    }

    /**
     * 搜索建议（自动补全）
     */
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam @Size(max = 100, message = "关键词不能超过100个字符") String keyword,
            @RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<String> suggestions = searchService.getSearchSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    /**
     * 记录搜索关键词（用于热词统计和历史记录）
     */
    @PostMapping("/record")
    public Result<Void> recordSearch(@RequestParam @Size(max = 100, message = "关键词不能超过100个字符") String keyword) {
        searchService.recordSearch(keyword);
        return Result.success();
    }
}
