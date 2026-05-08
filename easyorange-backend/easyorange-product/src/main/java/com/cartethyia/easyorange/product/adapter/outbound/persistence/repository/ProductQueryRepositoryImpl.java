package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.adapter.outbound.cache.ProductCacheConstant;
import com.cartethyia.easyorange.product.domain.constant.ProductConstant;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.HotKeywordDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.SearchHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.HotKeywordMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.SearchHistoryMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<ProductReadModel> searchProducts(String keyword, Long categoryId, Integer status,
                                          Integer pageNum, Integer pageSize) {
        return searchProducts(keyword, categoryId, status, null, null, null, null, pageNum, pageSize);
    }

    @Override
    public Page<ProductReadModel> searchProducts(String keyword, Long categoryId, Integer status,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          Integer conditionLevel, String sort,
                                          Integer pageNum, Integer pageSize) {
        Page<ProductDO> page = new Page<>(pageNum, pageSize);

        if (keyword != null && !keyword.isBlank()) {
            Integer searchStatus = status != null ? status : ProductStatus.ONLINE.getCode();
            Page<ProductDO> resultPage = productMapper.searchByFullText(
                    page, keyword, searchStatus, minPrice, maxPrice, conditionLevel);
            return convertToReadModelPage(resultPage);
        }

        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            List<Long> categoryIds = resolveCategoryIdsWithChildren(categoryId);
            wrapper.in(ProductDO::getCategoryId, categoryIds);
        }
        if (status != null) {
            wrapper.eq(ProductDO::getStatus, status);
        } else {
            wrapper.eq(ProductDO::getStatus, ProductStatus.ONLINE.getCode());
        }
        if (minPrice != null) {
            wrapper.ge(ProductDO::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(ProductDO::getPrice, maxPrice);
        }
        if (conditionLevel != null) {
            wrapper.eq(ProductDO::getConditionLevel, conditionLevel);
        }

        applySort(wrapper, sort);

        Page<ProductDO> productPage = productMapper.selectPage(page, wrapper);
        return convertToReadModelPage(productPage);
    }

    private List<Long> resolveCategoryIdsWithChildren(Long categoryId) {
        List<CategoryDO> children = categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getParentId, categoryId)
                        .eq(CategoryDO::getStatus, 1)
        );
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(categoryId);
        children.forEach(c -> ids.add(c.getId()));
        return ids;
    }

    @Override
    public Page<ProductReadModel> findProductsBySellerId(Long sellerId, Integer status,
                                                          Integer pageNum, Integer pageSize) {
        Page<ProductDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductDO::getUserId, sellerId);
        if (status != null) {
            wrapper.eq(ProductDO::getStatus, status);
        }
        wrapper.orderByDesc(ProductDO::getCreateTime);

        Page<ProductDO> productPage = productMapper.selectPage(page, wrapper);
        return convertToReadModelPage(productPage);
    }

    @Override
    public List<ProductReadModel> findProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ProductDO> products = productMapper.selectBatchIds(ids);
        return products.stream()
                .map(this::convertToReadModel)
                .collect(Collectors.toList());
    }

    @Override
    public ProductReadModel findProductById(Long id) {
        ProductDO product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        return convertToReadModel(product);
    }

    @Override
    public List<SearchHistoryReadModel> findSearchHistoryByUserId(Long userId, Integer limit) {
        int lim = limit != null ? limit : 20;

        String key = ProductCacheConstant.SEARCH_HISTORY_KEY_PREFIX + userId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, lim - 1);

        if (history != null && !history.isEmpty()) {
            List<SearchHistoryReadModel> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Object item : history) {
                String keyword = item.toString();
                if (seen.add(keyword)) {
                    result.add(new SearchHistoryReadModel(null, keyword, null));
                    if (result.size() >= lim) break;
                }
            }
            return result;
        }

        Page<SearchHistoryDO> historyPage = new Page<>(1, lim);
        Page<SearchHistoryDO> result = searchHistoryMapper.selectPage(historyPage,
                new LambdaQueryWrapper<SearchHistoryDO>()
                        .eq(SearchHistoryDO::getUserId, userId)
                        .orderByDesc(SearchHistoryDO::getSearchTime));

        return result.getRecords().stream()
                .map(h -> new SearchHistoryReadModel(h.getId(), h.getKeyword(), h.getSearchTime()))
                .collect(Collectors.toList());
    }

    @Override
    public List<HotKeywordReadModel> findHotKeywords(Integer limit) {
        int lim = limit != null ? limit : 10;

        Set<Object> topKeywords = redisTemplate.opsForZSet()
                .reverseRange(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, 0, lim - 1);

        if (topKeywords != null && !topKeywords.isEmpty()) {
            List<HotKeywordReadModel> result = new ArrayList<>();
            for (Object keyword : topKeywords) {
                Double score = redisTemplate.opsForZSet().score(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, keyword);
                int count = score != null ? score.intValue() : 0;
                result.add(new HotKeywordReadModel(null, keyword.toString(), count, calculateHotLevel(count)));
            }
            return result;
        }

        Page<HotKeywordDO> keywordPage = new Page<>(1, lim);
        Page<HotKeywordDO> keywordResult = hotKeywordMapper.selectPage(keywordPage,
                new LambdaQueryWrapper<HotKeywordDO>()
                        .orderByDesc(HotKeywordDO::getSearchCount));

        return keywordResult.getRecords().stream()
                .map(k -> new HotKeywordReadModel(k.getId(), k.getKeyword(), k.getSearchCount(), calculateHotLevel(k.getSearchCount())))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findSearchSuggestions(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        Set<Object> allKeywords = redisTemplate.opsForZSet()
                .reverseRange(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, 0, -1);

        if (allKeywords != null && !allKeywords.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            return allKeywords.stream()
                    .map(Object::toString)
                    .filter(k -> k.toLowerCase().contains(lowerKeyword))
                    .limit(limit != null ? limit : 10)
                    .collect(Collectors.toList());
        }

        int suggestionLimit = limit != null ? limit : 10;
        Page<HotKeywordDO> suggestionPage = new Page<>(1, suggestionLimit);
        Page<HotKeywordDO> suggestionResult = hotKeywordMapper.selectPage(suggestionPage,
                new LambdaQueryWrapper<HotKeywordDO>()
                        .like(HotKeywordDO::getKeyword, keyword)
                        .orderByDesc(HotKeywordDO::getSearchCount));

        return suggestionResult.getRecords().stream()
                .map(HotKeywordDO::getKeyword)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerReadModel> findSellersByIds(Set<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return List.of();
        }
        return productMapper.selectSellersByIds(sellerIds).stream()
                .map(s -> new SellerReadModel(s.id(), s.username(), s.nickName(), s.avatar()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryInfo> findCategoriesByIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        List<CategoryDO> categories = categoryMapper.selectBatchIds(categoryIds);
        return categories.stream()
                .map(c -> new CategoryInfo(c.getId(), c.getName(), c.getParentId(), c.getLevel(), c.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDetailInfo> findDetailsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<ProductDetailDO> details = productDetailMapper.selectDetailsByProductIds(productIds);
        return details.stream()
                .map(d -> new ProductDetailInfo(d.getProductId(), d.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductImageInfo> findImagesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<ProductImageDO> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImageDO>()
                        .in(ProductImageDO::getProductId, productIds)
                        .orderByAsc(ProductImageDO::getSortOrder)
        );
        return images.stream()
                .map(img -> new ProductImageInfo(img.getProductId(), img.getImageUrl(), img.getSortOrder(), img.getIsMain() != null && img.getIsMain().equals(1)))
                .collect(Collectors.toList());
    }

    @Override
    public void saveSearchHistory(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String key = ProductCacheConstant.searchHistoryKey(userId);
        redisTemplate.opsForList().remove(key, 0, keyword);
        redisTemplate.opsForList().leftPush(key, keyword);
        redisTemplate.opsForList().trim(key, 0, 19);

        SearchHistoryDO historyDO = new SearchHistoryDO();
        historyDO.setUserId(userId);
        historyDO.setKeyword(keyword);
        searchHistoryMapper.insert(historyDO);
    }

    @Override
    public void clearSearchHistory(Long userId) {
        String key = ProductCacheConstant.searchHistoryKey(userId);
        redisTemplate.delete(key);

        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistoryDO>()
                        .eq(SearchHistoryDO::getUserId, userId)
        );
    }

    @Override
    public void deleteSearchHistoryById(Long historyId, Long userId) {
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistoryDO>()
                        .eq(SearchHistoryDO::getId, historyId)
                        .eq(SearchHistoryDO::getUserId, userId)
        );
    }

    @Override
    public long countByStatus(Integer status) {
        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductDO::getStatus, status);
        return productMapper.selectCount(wrapper);
    }

    private Page<ProductReadModel> convertToReadModelPage(Page<ProductDO> productPage) {
        Page<ProductReadModel> readModelPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        readModelPage.setRecords(productPage.getRecords().stream()
                .map(this::convertToReadModel)
                .collect(Collectors.toList()));
        return readModelPage;
    }

    private ProductReadModel convertToReadModel(ProductDO product) {
        return new ProductReadModel(
                product.getId(),
                product.getUserId(),
                null,
                null,
                product.getCategoryId(),
                null,
                product.getName(),
                null,
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus(),
                ProductStatus.getDescByCode(product.getStatus()),
                product.getViewCount(),
                product.getConditionLevel(),
                null,
                null,
                null,
                List.of(),
                "",
                product.getCreateTime(),
                product.getUpdateTime()
        );
    }

    private void applySort(LambdaQueryWrapper<ProductDO> wrapper, String sort) {
        if (sort == null || sort.isBlank() || "default".equals(sort)) {
            wrapper.orderByDesc(ProductDO::getCreateTime);
            return;
        }
        switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(ProductDO::getPrice);
            case "price_desc" -> wrapper.orderByDesc(ProductDO::getPrice);
            case "newest" -> wrapper.orderByDesc(ProductDO::getCreateTime);
            case "view", "popular" -> wrapper.orderByDesc(ProductDO::getViewCount);
            default -> wrapper.orderByDesc(ProductDO::getCreateTime);
        }
    }

    private Integer calculateHotLevel(int searchCount) {
        if (searchCount >= ProductConstant.HOT_LEVEL_5_THRESHOLD) return 5;
        if (searchCount >= ProductConstant.HOT_LEVEL_4_THRESHOLD) return 4;
        if (searchCount >= ProductConstant.HOT_LEVEL_3_THRESHOLD) return 3;
        if (searchCount >= ProductConstant.HOT_LEVEL_2_THRESHOLD) return 2;
        if (searchCount >= ProductConstant.HOT_LEVEL_1_THRESHOLD) return 1;
        return 0;
    }
}
