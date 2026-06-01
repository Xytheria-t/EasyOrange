package com.cartethyia.easyorange.product.application.query.handler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.FacetBucketResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.domain.port.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.domain.port.SearchResult;
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

    @Transactional(readOnly = true)
    public SearchPageResponse<ProductResponse> handleSearch(ProductSearchRequest request) {
        if (searchQueryPort.isPresent()) {
            ProductSearchQueryPort.ProductSearchQuery query = new ProductSearchQueryPort.ProductSearchQuery(
                    request.getKeyword(),
                    request.getCategoryId(),
                    request.getStatus(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getConditionLevel(),
                    request.getSortField(),
                    request.getPageNum() != null ? request.getPageNum() : 1,
                    request.getPageSize() != null ? request.getPageSize() : 20,
                    null,
                    false
            );
            SearchResult searchResult = searchQueryPort.get().search(query);
            List<ProductResponse> responses = searchResult.records().stream()
                    .map(this::toProductResponse)
                    .collect(Collectors.toList());
            List<FacetBucketResponse> facets = mergeFacets(searchResult);
            return SearchPageResponse.of(responses, searchResult.total(), searchResult.pageNum(),
                    searchResult.pageSize(), facets);
        }

        PageResult<ProductReadModel> page = productQueryRepository.searchProducts(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getPageNum() != null ? request.getPageNum() : 1,
                request.getPageSize() != null ? request.getPageSize() : 20
        );

        List<ProductResponse> responses = page.records().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return SearchPageResponse.of(responses, page.total(), page.current(), page.size());
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getMySearchHistory(Integer limit) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
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
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        productQueryRepository.clearSearchHistory(userId);
    }

    public void deleteSearchHistory(Long historyId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
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
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
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
        var list = new ArrayList<FacetBucketResponse>();
        result.categoryFacets().forEach(fb ->
                list.add(new FacetBucketResponse("category_" + fb.key(), fb.count())));
        result.conditionFacets().forEach(fb ->
                list.add(new FacetBucketResponse("condition_" + fb.key(), fb.count())));
        result.priceRangeFacets().forEach(fb ->
                list.add(new FacetBucketResponse("price_" + fb.key(), fb.count())));
        return List.copyOf(list);
    }
}
