package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.product.adapter.inbound.web.assembler.ProductSearchAssembler;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.HotKeywordResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchHistoryResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.ProductSearchQueryHandler;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
// 故意不加 @Validated：控制器级 @Validated 会关掉 Spring MVC 内建方法校验（改为 AOP 代理 + ConstraintViolationException），
// 参数上的 @Max/@Size 交给内建方法校验，抛 HandlerMethodValidationException 后由 GlobalExceptionHandler 统一映射 400
public class ProductSearchController {

    private final ProductSearchQueryHandler searchQueryHandler;
    private final ProductSearchAssembler searchAssembler;

    @GetMapping
    @Operation(summary = "公开搜索，ES 未启用或不可达时回退 MySQL（回退后 facets 为空）")
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
        return Result.success(searchAssembler.toSearchPageResponse(result));
    }

    @GetMapping("/history")
    @Operation(summary = "当前用户搜索历史（Redis 列表优先，miss 回源 DB）")
    public Result<List<SearchHistoryResponse>> getMySearchHistory(
            @AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "20") @Max(50) Integer limit) {
        List<SearchHistoryReadModel> histories = searchQueryHandler.getMySearchHistory(user.userId(), limit);
        return Result.success(searchAssembler.toSearchHistoryResponses(histories));
    }

    @DeleteMapping("/history")
    @Operation(summary = "清空当前用户的搜索历史（Redis 与 DB 同删）")
    public Result<Void> clearMySearchHistory(@AuthenticationPrincipal AuthUser user) {
        searchQueryHandler.clearMySearchHistory(user.userId());
        return Result.success();
    }

    @DeleteMapping("/history/{historyId}")
    @Operation(summary = "删除自己的单条搜索历史（条件带 userId，越权即无操作）")
    public Result<Void> deleteSearchHistory(@AuthenticationPrincipal AuthUser user, @PathVariable String historyId) {
        searchQueryHandler.deleteSearchHistory(user.userId(), historyId);
        return Result.success();
    }

    @GetMapping("/hot")
    @Operation(summary = "热词榜（Redis ZSet 优先，miss 回源 DB 按搜索次数倒序）")
    public Result<List<HotKeywordResponse>> getHotKeywords(@RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<HotKeywordReadModel> keywords = searchQueryHandler.getHotKeywords(limit);
        return Result.success(searchAssembler.toHotKeywordResponses(keywords));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "搜索建议（热词包含匹配，Redis 全量过滤优先）")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam @Size(max = 100, message = "关键词不能超过 100 个字符") String keyword,
            @RequestParam(defaultValue = "10") @Max(50) Integer limit) {
        List<String> suggestions = searchQueryHandler.getSearchSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    @PostMapping("/record")
    @Operation(summary = "记录搜索关键词（历史去重截断，热词计数失败不阻断）")
    public Result<Void> recordSearch(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam @Size(max = 100, message = "关键词不能超过 100 个字符") String keyword) {
        searchQueryHandler.recordSearch(user.userId(), keyword);
        return Result.success();
    }
}
