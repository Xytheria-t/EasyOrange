package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.ProductSearchCriteria;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.application.service.SearchHistoryBufferAppService;
import com.cartethyia.easyorange.product.adapter.outbound.cache.ProductCacheConstant;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.HotKeywordDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.SearchHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.HotKeywordMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.SearchHistoryMapper;
import com.cartethyia.easyorange.product.domain.constant.ProductConstant;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final SearchHistoryBufferAppService searchHistoryBufferService;
    private final CategoryCachePort categoryCachePort;

    // ===================== 商品查询 =====================

    @Override
    public PageResult<ProductReadModel> searchProducts(ProductSearchCriteria criteria) {
        var page = new Page<ProductDO>(criteria.effectivePageNum(), criteria.effectivePageSize());

        String keyword = criteria.keyword();
        String categoryId = criteria.categoryId();
        String status = criteria.status();
        BigDecimal minPrice = criteria.minPrice();
        BigDecimal maxPrice = criteria.maxPrice();
        String conditionLevel = criteria.conditionLevel();
        String sort = criteria.sort();
        Boolean hasDiscount = criteria.hasDiscount();

        if (keyword != null && !keyword.isBlank()) {
            var searchCriteria = new ProductMapper.ProductSearchCriteria(
                    keyword,
                    status != null ? ProductStatus.fromCode(status) : ProductStatus.ONLINE,
                    minPrice, maxPrice,
                    conditionLevel != null ? ConditionLevel.fromCode(conditionLevel) : null,
                    hasDiscount);
            return convertToReadModelPage(productMapper.searchByFullText(page, searchCriteria));
        }

        var wrapper = ChainWrappers.lambdaQueryChain(productMapper);
        if (categoryId != null) {
            wrapper.in(ProductDO::getCategoryId, resolveCategoryIdsWithChildren(categoryId));
        }
        wrapper.eq(ProductDO::getStatus, Objects.requireNonNullElse(status, ProductStatus.ONLINE.getCode()));
        if (minPrice != null) wrapper.ge(ProductDO::getPrice, minPrice);
        if (maxPrice != null) wrapper.le(ProductDO::getPrice, maxPrice);
        if (conditionLevel != null) wrapper.eq(ProductDO::getConditionLevel, conditionLevel);
        if (Boolean.TRUE.equals(hasDiscount)) {
            wrapper.apply("original_price IS NOT NULL AND original_price > price");
        }
        applySort(wrapper, sort);

        return convertToReadModelPage(wrapper.page(page));
    }

    @Override
    public PageResult<ProductReadModel> findProductsBySellerId(String sellerId, String status,
                                                                Integer pageNum, Integer pageSize) {
        var wrapper = ChainWrappers.lambdaQueryChain(productMapper);
        wrapper.eq(ProductDO::getUserId, sellerId);
        if (status != null) wrapper.eq(ProductDO::getStatus, status);
        wrapper.orderByDesc(ProductDO::getCreateTime);
        return convertToReadModelPage(wrapper.page(new Page<>(pageNum, pageSize)));
    }

    @Override
    public List<ProductReadModel> findProductsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ChainWrappers.lambdaQueryChain(productMapper)
                .in(ProductDO::getId, ids).list().stream()
                .map(this::convertToReadModel)
                .toList();
    }

    @Override
    public ProductReadModel findProductById(String id) {
        var product = productMapper.selectById(id);
        return product != null ? convertToReadModel(product) : null;
    }

    // ===================== 关联数据查询 =====================

    @Override
    public List<CategoryInfo> findCategoriesByIds(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return List.of();
        return ChainWrappers.lambdaQueryChain(categoryMapper)
                .in(CategoryDO::getId, categoryIds).list().stream()
                .map(c -> new CategoryInfo(c.getId(), c.getName(), c.getParentId(), c.getLevel(), c.getSortOrder()))
                .toList();
    }

    @Override
    public List<ProductDetailInfo> findDetailsByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return productDetailMapper.selectDetailsByProductIds(productIds).stream()
                .map(d -> new ProductDetailInfo(d.getProductId(), d.getDescription()))
                .toList();
    }

    @Override
    public List<ProductImageInfo> findImagesByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return ChainWrappers.lambdaQueryChain(productImageMapper)
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder).list().stream()
                .map(img -> new ProductImageInfo(img.getProductId(), img.getImageUrl(), img.getSortOrder(),
                        Objects.equals(img.getIsMain(), 1)))
                .toList();
    }

    @Override
    public List<SellerReadModel> findSellersByIds(Set<String> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) return List.of();
        return productMapper.selectSellersByIds(sellerIds);
    }

    // ===================== 搜索历史与热词 =====================

    @Override
    public List<SearchHistoryReadModel> findSearchHistoryByUserId(String userId, Integer limit) {
        int lim = limit != null ? limit : ProductCacheConstant.SEARCH_HISTORY_MAX_SIZE;
        String key = ProductCacheConstant.SEARCH_HISTORY_KEY_PREFIX + userId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, lim - 1);

        if (history != null && !history.isEmpty()) {
            return history.stream()
                    .map(Object::toString)
                    .distinct()
                    .limit(lim)
                    .map(k -> new SearchHistoryReadModel(null, k, null))
                    .toList();
        }

        return ChainWrappers.lambdaQueryChain(searchHistoryMapper)
                .eq(SearchHistoryDO::getUserId, userId)
                .orderByDesc(SearchHistoryDO::getSearchTime)
                .page(new Page<>(1, lim)).getRecords().stream()
                .map(h -> new SearchHistoryReadModel(h.getId(), h.getKeyword(), h.getSearchTime()))
                .toList();
    }

    @Override
    public List<HotKeywordReadModel> findHotKeywords(Integer limit) {
        int lim = limit != null ? limit : 10;
        Set<Object> topKeywords = redisTemplate.opsForZSet()
                .reverseRange(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, 0, lim - 1);

        if (topKeywords != null && !topKeywords.isEmpty()) {
            return topKeywords.stream()
                    .map(k -> {
                        int count = Optional.ofNullable(redisTemplate.opsForZSet()
                                        .score(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, k))
                                .map(Number::intValue).orElse(0);
                        return new HotKeywordReadModel(null, k.toString(), count, calculateHotLevel(count));
                    })
                    .toList();
        }

        return ChainWrappers.lambdaQueryChain(hotKeywordMapper)
                .orderByDesc(HotKeywordDO::getSearchCount)
                .page(new Page<>(1, lim)).getRecords().stream()
                .map(k -> new HotKeywordReadModel(k.getId(), k.getKeyword(), k.getSearchCount(),
                        calculateHotLevel(k.getSearchCount())))
                .toList();
    }

    @Override
    public List<String> findSearchSuggestions(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) return List.of();

        int lim = limit != null ? limit : 10;
        Set<Object> allKeywords = redisTemplate.opsForZSet()
                .reverseRange(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, 0, -1);

        if (allKeywords != null && !allKeywords.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            return allKeywords.stream()
                    .map(Object::toString)
                    .filter(k -> k.toLowerCase().contains(lowerKeyword))
                    .limit(lim)
                    .toList();
        }

        return ChainWrappers.lambdaQueryChain(hotKeywordMapper)
                .like(HotKeywordDO::getKeyword, keyword)
                .orderByDesc(HotKeywordDO::getSearchCount)
                .page(new Page<>(1, lim)).getRecords().stream()
                .map(HotKeywordDO::getKeyword)
                .toList();
    }

    // ===================== 搜索历史写入 =====================

    @Override
    public void saveSearchHistory(String userId, String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        String key = ProductCacheConstant.searchHistoryKey(userId);
        redisTemplate.opsForList().remove(key, 0, keyword);
        redisTemplate.opsForList().leftPush(key, keyword);
        redisTemplate.opsForList().trim(key, 0, ProductCacheConstant.SEARCH_HISTORY_MAX_SIZE - 1);

        searchHistoryBufferService.addToBuffer(userId, keyword);
    }

    @Override
    public void clearSearchHistory(String userId) {
        redisTemplate.delete(ProductCacheConstant.searchHistoryKey(userId));
        ChainWrappers.lambdaUpdateChain(searchHistoryMapper)
                .eq(SearchHistoryDO::getUserId, userId).remove();
    }

    @Override
    public void deleteSearchHistoryById(String historyId, String userId) {
        ChainWrappers.lambdaUpdateChain(searchHistoryMapper)
                .eq(SearchHistoryDO::getId, historyId)
                .eq(SearchHistoryDO::getUserId, userId)
                .remove();
    }

    // ===================== 统计 =====================

    @Override
    public long countByStatus(String status) {
        return ChainWrappers.lambdaQueryChain(productMapper)
                .eq(ProductDO::getStatus, status).count();
    }

    // ===================== 私有辅助方法 =====================

    private PageResult<ProductReadModel> convertToReadModelPage(Page<ProductDO> productPage) {
        var records = productPage.getRecords().stream()
                .map(this::convertToReadModel).toList();
        return PageResult.of(records, productPage.getTotal(),
                (int) productPage.getCurrent(), (int) productPage.getSize());
    }

    private ProductReadModel convertToReadModel(ProductDO product) {
        return ProductReadModel.builder()
                .id(product.getId())
                .sellerId(product.getUserId())
                .categoryId(product.getCategoryId())
                .title(product.getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .status(product.getStatus().getCode())
                .statusDesc(product.getStatus().getDesc())
                .views(product.getViewCount())
                .condition(product.getConditionLevel() != null ? product.getConditionLevel().getCode() : null)
                .images(List.of())
                .mainImageUrl("")
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime())
                .build();
    }

    private void applySort(LambdaQueryChainWrapper<ProductDO> wrapper, String sort) {
        if (sort == null || sort.isBlank() || "default".equals(sort)) {
            wrapper.orderByDesc(ProductDO::getCreateTime);
            return;
        }
        switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(ProductDO::getPrice);
            case "price_desc" -> wrapper.orderByDesc(ProductDO::getPrice);
            case "view", "popular" -> wrapper.orderByDesc(ProductDO::getViewCount);
            default -> wrapper.orderByDesc(ProductDO::getCreateTime);
        }
    }

    private List<String> resolveCategoryIdsWithChildren(String categoryId) {
        var ids = new ArrayList<String>();
        ids.add(categoryId);
        categoryCachePort.getCategoriesByParentId(categoryId).stream()
                .filter(c -> c.status() != null && c.status() == 1)
                .map(CategoryReadModel::id)
                .forEach(ids::add);
        return ids;
    }

    private static Integer calculateHotLevel(int searchCount) {
        if (searchCount >= ProductConstant.HOT_LEVEL_5_THRESHOLD) return 5;
        if (searchCount >= ProductConstant.HOT_LEVEL_4_THRESHOLD) return 4;
        if (searchCount >= ProductConstant.HOT_LEVEL_3_THRESHOLD) return 3;
        if (searchCount >= ProductConstant.HOT_LEVEL_2_THRESHOLD) return 2;
        if (searchCount >= ProductConstant.HOT_LEVEL_1_THRESHOLD) return 1;
        return 0;
    }
}
