package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.AiSearchEnhancerPort;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchQueryHandler {

    private final ProductQueryRepository productQueryRepository;
    private final ObjectProvider<ProductSearchQueryPort> searchQueryPort;
    private final ObjectProvider<AiSearchEnhancerPort> aiSearchEnhancer;

    @Transactional(readOnly = true)
    public ProductSearchResult search(ProductSearchCriteria criteria, boolean aiEnhanced) {
        List<ProductReadModel> readModels;
        PageResult<ProductReadModel> page;
        List<FacetBucket> facets = List.of();

        // ES 检索 adapter 未配置（elasticsearch.enabled=false）时回退 DB 检索
        var esPort = searchQueryPort.getIfAvailable();
        if (esPort != null) {
            var query = new ProductSearchQueryPort.ProductSearchQuery(
                    criteria.keyword(),
                    criteria.categoryId(),
                    criteria.status(),
                    criteria.minPrice(),
                    criteria.maxPrice(),
                    criteria.conditionLevel(),
                    criteria.sort(),
                    criteria.effectivePageNum(),
                    criteria.effectivePageSize(),
                    null,
                    false);
            var searchResult = esPort.search(query);
            readModels = searchResult.records();
            facets = mergeFacetsList(searchResult);
            page = PageResult.of(
                    searchResult.records(), searchResult.total(), searchResult.current(), searchResult.size());
        } else {
            page = productQueryRepository.searchProducts(criteria);
            readModels = page.records();
        }

        var enhancer = aiSearchEnhancer.getIfAvailable();
        var aiEnhancement = aiEnhanced && enhancer != null && !readModels.isEmpty()
                ? enhancer.tryEnhance(criteria.keyword(), takeTop(readModels, 5))
                        .orElse(null)
                : null;

        return new ProductSearchResult(page, facets, aiEnhancement);
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryReadModel> getMySearchHistory(String userId, Integer limit) {
        return productQueryRepository.findSearchHistoryByUserId(userId, limit);
    }

    public void clearMySearchHistory(String userId) {
        productQueryRepository.clearSearchHistory(userId);
    }

    public void deleteSearchHistory(String userId, String historyId) {
        productQueryRepository.deleteSearchHistoryById(historyId, userId);
    }

    @Transactional(readOnly = true)
    public List<HotKeywordReadModel> getHotKeywords(Integer limit) {
        return productQueryRepository.findHotKeywords(limit);
    }

    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, Integer limit) {
        return productQueryRepository.findSearchSuggestions(keyword, limit);
    }

    public void recordSearch(String userId, String keyword) {
        productQueryRepository.saveSearchHistory(userId, keyword);
    }

    private static List<FacetBucket> mergeFacetsList(
            com.cartethyia.easyorange.product.application.port.query.SearchResult result) {
        var list = new ArrayList<FacetBucket>();
        result.categoryFacets()
                .forEach(fb -> list.add(new FacetBucket("category_" + fb.key(), fb.label(), fb.count())));
        result.conditionFacets()
                .forEach(fb -> list.add(new FacetBucket("condition_" + fb.key(), fb.label(), fb.count())));
        result.priceRangeFacets().forEach(fb -> list.add(new FacetBucket("price_" + fb.key(), fb.label(), fb.count())));
        return List.copyOf(list);
    }

    private static List<ProductReadModel> takeTop(List<ProductReadModel> items, int n) {
        return items.size() <= n ? items : items.subList(0, n);
    }
}
