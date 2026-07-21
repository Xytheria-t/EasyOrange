package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.FacetBucketResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.domain.port.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.domain.port.SearchResult;
import com.cartethyia.easyorange.product.domain.port.AiSearchEnhancerPort;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.HotKeywordResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchHandler {

    private final ProductQueryRepository productQueryRepository;
    private final Optional<ProductSearchQueryPort> searchQueryPort;
    private final Optional<AiSearchEnhancerPort> aiSearchEnhancer;

    @Transactional(readOnly = true)
    public SearchPageResponse<ProductResponse> handleSearch(ProductSearchRequest request) {
        List<ProductReadModel> readModels;
        SearchPageResponse<ProductResponse> result;

        if (searchQueryPort.isPresent()) {
            var query = new ProductSearchQueryPort.ProductSearchQuery(
                    request.getKeyword(), request.getCategoryId(), request.getStatus(),
                    request.getMinPrice(), request.getMaxPrice(), request.getConditionLevel(),
                    request.getSortField(),
                    request.getPageNum() != null ? request.getPageNum() : 1,
                    request.getPageSize() != null ? request.getPageSize() : 20,
                    null, false);
            var searchResult = searchQueryPort.get().search(query);
            readModels = searchResult.records();
            var responses = readModels.stream().map(this::toProductResponse).toList();
            var facets = mergeFacets(searchResult);
            result = SearchPageResponse.of(responses, searchResult.total(), searchResult.current(),
                    searchResult.size(), facets);
        } else {
            var page = productQueryRepository.searchProducts(
                    request.getKeyword(), request.getCategoryId(), request.getStatus(),
                    request.getPageNum() != null ? request.getPageNum() : 1,
                    request.getPageSize() != null ? request.getPageSize() : 20);
            readModels = page.records();
            var responses = readModels.stream().map(this::toProductResponse).toList();
            result = SearchPageResponse.of(page, responses);
        }

        // AI search enhancement — use original ReadModels directly, no back-conversion
        if (request.isAiEnhanced()
                && aiSearchEnhancer.isPresent()
                && !readModels.isEmpty()) {
            var topProducts = readModels.size() <= 5
                    ? readModels
                    : readModels.subList(0, 5);
            var enhancement = aiSearchEnhancer.get().tryEnhance(request.getKeyword(), topProducts);
            if (enhancement.isPresent()) {
                result = result.withAiEnhancement(enhancement.get());
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getMySearchHistory(Integer limit) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        List<SearchHistoryReadModel> histories = productQueryRepository.findSearchHistoryByUserId(userId, limit);
        return histories.stream()
                .map(h -> SearchHistoryResponse.builder()
                        .id(h.id())
                        .keyword(h.keyword())
                        .createTime(h.createTime())
                        .build())
                .collect(Collectors.toList());
    }

    public void clearMySearchHistory() {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        productQueryRepository.clearSearchHistory(userId);
    }

    public void deleteSearchHistory(String historyId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        productQueryRepository.deleteSearchHistoryById(historyId, userId);
    }

    @Transactional(readOnly = true)
    public List<HotKeywordResponse> getHotKeywords(Integer limit) {
        List<HotKeywordReadModel> keywords = productQueryRepository.findHotKeywords(limit);
        return keywords.stream()
                .map(k -> HotKeywordResponse.builder()
                        .id(k.id())
                        .keyword(k.keyword())
                        .searchCount(k.searchCount())
                        .hotLevel(k.hotLevel())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, Integer limit) {
        return productQueryRepository.findSearchSuggestions(keyword, limit);
    }

    public void recordSearch(String keyword) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        productQueryRepository.saveSearchHistory(userId, keyword);
    }

    private ProductResponse toProductResponse(ProductReadModel model) {
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

    private List<FacetBucketResponse> mergeFacets(SearchResult result) {
        List<FacetBucketResponse> list = new ArrayList<>();
        result.categoryFacets().forEach(fb ->
                list.add(new FacetBucketResponse("category_" + fb.key(), fb.count())));
        result.conditionFacets().forEach(fb ->
                list.add(new FacetBucketResponse("condition_" + fb.key(), fb.count())));
        result.priceRangeFacets().forEach(fb ->
                list.add(new FacetBucketResponse("price_" + fb.key(), fb.count())));
        return List.copyOf(list);
    }
}
