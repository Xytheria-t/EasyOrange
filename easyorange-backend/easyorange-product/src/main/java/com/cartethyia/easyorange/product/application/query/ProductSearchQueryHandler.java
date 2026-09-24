package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.application.port.query.QueryEmbeddingPort;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
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
    private final ObjectProvider<QueryEmbeddingPort> queryEmbedding;

    @Transactional(readOnly = true)
    public ProductSearchResult search(ProductSearchCriteria criteria, boolean aiEnhanced) {
        PageResult<ProductReadModel> page;
        List<FacetBucket> facets = List.of();

        // ES 检索 adapter 未配置（elasticsearch.enabled=false）时回退 DB 检索
        var esPort = searchQueryPort.getIfAvailable();
        if (esPort != null) {
            // 与 DB 回退路径一致：公开搜索未显式指定状态时默认只展示上架商品
            var effectiveStatus = criteria.status() != null ? criteria.status() : ProductStatus.ONLINE.getCode();
            var embedding = queryEmbeddingFor(criteria, aiEnhanced);
            var query = new ProductSearchQueryPort.ProductSearchQuery(
                    criteria.keyword(),
                    criteria.categoryId(),
                    effectiveStatus,
                    criteria.minPrice(),
                    criteria.maxPrice(),
                    criteria.conditionLevel(),
                    criteria.sort(),
                    criteria.effectivePageNum(),
                    criteria.effectivePageSize(),
                    embedding,
                    !embedding.isEmpty());
            var searchResult = esPort.search(query);
            facets = mergeFacetsList(searchResult);
            page = PageResult.of(
                    searchResult.records(), searchResult.total(), searchResult.current(), searchResult.size());
        } else {
            page = productQueryRepository.searchProducts(criteria);
        }

        return new ProductSearchResult(page, facets);
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

    /**
     * kNN 那一路的查询向量；返回空列表即关掉该路，检索退化为纯 BM25。
     * <p>
     * 只在用户显式开启语义检索（{@code aiEnhanced}）且按相关性排序时向量化：
     * 词面命中本就精准的关键词搜索没有语义召回的必要 —— kNN 缺相似度下限时在小语料上会召回全库，
     * 融合后把不相关商品顶进结果，还会白付一次 embedding 调用；
     * 用户显式点了价格 / 最新 / 热度同理，排序语义压过相关性，融合排名会和点选的排序打架。
     * 关键词为空（纯筛选浏览）也不走 kNN —— 没有检索意图可编码，
     * 而且 kNN 缺了 query 子句只能退化成 match_all，等于按过滤条件随机取一批。
     */
    private List<Float> queryEmbeddingFor(ProductSearchCriteria criteria, boolean aiEnhanced) {
        if (!aiEnhanced
                || !ProductSearchQueryPort.isRelevanceSort(criteria.sort())
                || criteria.keyword() == null
                || criteria.keyword().isBlank()) {
            return List.of();
        }
        var port = queryEmbedding.getIfAvailable();
        return port == null ? List.of() : port.embed(criteria.keyword());
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
}
