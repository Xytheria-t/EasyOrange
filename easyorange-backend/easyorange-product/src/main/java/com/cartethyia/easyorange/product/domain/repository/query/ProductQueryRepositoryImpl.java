package com.cartethyia.easyorange.product.domain.repository.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.constant.ProductConstants;
import com.cartethyia.easyorange.product.dto.vo.HotKeywordVO;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.dto.vo.SearchHistoryVO;
import com.cartethyia.easyorange.product.entity.HotKeyword;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.SearchHistory;
import com.cartethyia.easyorange.product.enums.ProductStatus;
import com.cartethyia.easyorange.product.mapper.HotKeywordMapper;
import com.cartethyia.easyorange.product.mapper.ProductMapper;
import com.cartethyia.easyorange.product.mapper.SearchHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final ProductMapper productMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<ProductVO> searchProducts(String keyword, Long categoryId, Integer status,
                                          Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);

        if (keyword != null && !keyword.isBlank()) {
            Page<Product> resultPage = productMapper.searchByFullText(
                    page, keyword, status != null ? status : ProductStatus.ONLINE.getCode());

            return convertToProductVOPage(resultPage);
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        } else {
            wrapper.eq(Product::getStatus, ProductStatus.ONLINE.getCode());
        }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        return convertToProductVOPage(productPage);
    }

    @Override
    public List<ProductVO> findProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Product> products = productMapper.selectBatchIds(ids);
        return products.stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVO findProductVOById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        return convertToProductVO(product);
    }

    @Override
    public List<SearchHistoryVO> findSearchHistoryByUserId(Long userId, Integer limit) {
        int lim = limit != null ? limit : 20;

        String key = ProductConstants.SEARCH_HISTORY_KEY_PREFIX + userId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, lim - 1);

        if (history != null && !history.isEmpty()) {
            List<SearchHistoryVO> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Object item : history) {
                String keyword = item.toString();
                if (seen.add(keyword)) {
                    result.add(SearchHistoryVO.builder()
                            .keyword(keyword)
                            .build());
                    if (result.size() >= lim) break;
                }
            }
            return result;
        }

        List<SearchHistory> histories = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getSearchTime)
                        .last("LIMIT " + lim));

        return histories.stream()
                .map(h -> SearchHistoryVO.builder()
                        .id(h.getId())
                        .keyword(h.getKeyword())
                        .createTime(h.getSearchTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<HotKeywordVO> findHotKeywords(Integer limit) {
        int lim = limit != null ? limit : 10;

        Set<Object> topKeywords = redisTemplate.opsForZSet()
                .reverseRange(ProductConstants.HOT_KEYWORD_ZSET_KEY, 0, lim - 1);

        if (topKeywords != null && !topKeywords.isEmpty()) {
            List<HotKeywordVO> result = new ArrayList<>();
            for (Object keyword : topKeywords) {
                Double score = redisTemplate.opsForZSet().score(ProductConstants.HOT_KEYWORD_ZSET_KEY, keyword);
                int count = score != null ? score.intValue() : 0;
                result.add(HotKeywordVO.builder()
                        .keyword(keyword.toString())
                        .searchCount(count)
                        .hotLevel(calculateHotLevel(count))
                        .build());
            }
            return result;
        }

        List<HotKeyword> keywords = hotKeywordMapper.selectList(
                new LambdaQueryWrapper<HotKeyword>()
                        .orderByDesc(HotKeyword::getSearchCount)
                        .last("LIMIT " + lim));

        return keywords.stream()
                .map(k -> HotKeywordVO.builder()
                        .id(k.getId())
                        .keyword(k.getKeyword())
                        .searchCount(k.getSearchCount())
                        .hotLevel(calculateHotLevel(k.getSearchCount()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findSearchSuggestions(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        Set<Object> allKeywords = redisTemplate.opsForZSet()
                .reverseRange(ProductConstants.HOT_KEYWORD_ZSET_KEY, 0, -1);

        if (allKeywords != null && !allKeywords.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            return allKeywords.stream()
                    .map(Object::toString)
                    .filter(k -> k.toLowerCase().contains(lowerKeyword))
                    .limit(limit != null ? limit : 10)
                    .collect(Collectors.toList());
        }

        List<HotKeyword> keywords = hotKeywordMapper.selectList(
                new LambdaQueryWrapper<HotKeyword>()
                        .like(HotKeyword::getKeyword, keyword)
                        .orderByDesc(HotKeyword::getSearchCount)
                        .last("LIMIT " + (limit != null ? limit : 10)));

        return keywords.stream()
                .map(HotKeyword::getKeyword)
                .collect(Collectors.toList());
    }

    private Page<ProductVO> convertToProductVOPage(Page<Product> productPage) {
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    private ProductVO convertToProductVO(Product product) {
        return ProductVO.builder()
                .id(product.getId())
                .title(product.getName())
                .price(product.getPrice())
                .mainImageUrl("")
                .build();
    }

    private Integer calculateHotLevel(int searchCount) {
        if (searchCount >= ProductConstants.HOT_LEVEL_5_THRESHOLD) return 5;
        if (searchCount >= ProductConstants.HOT_LEVEL_4_THRESHOLD) return 4;
        if (searchCount >= ProductConstants.HOT_LEVEL_3_THRESHOLD) return 3;
        if (searchCount >= ProductConstants.HOT_LEVEL_2_THRESHOLD) return 2;
        if (searchCount >= ProductConstants.HOT_LEVEL_1_THRESHOLD) return 1;
        return 0;
    }
}
