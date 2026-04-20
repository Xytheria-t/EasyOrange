package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.constant.ProductConstants;
import com.cartethyia.easyorange.product.dto.request.ProductSearchRequest;
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
import com.cartethyia.easyorange.product.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory> implements SearchService {

    private final SearchHistoryMapper searchHistoryMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<ProductVO> searchProducts(ProductSearchRequest request) {
        // 全文搜索优先（数据库层分页）
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            Page<Product> searchPage = new Page<>(request.getPageNum(), request.getPageSize());
            Page<Product> resultPage = productMapper.searchByFullText(
                    searchPage, request.getKeyword(), ProductStatus.ONLINE.getCode());

            Page<ProductVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            voPage.setRecords(resultPage.getRecords().stream()
                    .map(p -> ProductVO.builder()
                            .id(p.getId())
                            .title(p.getName())
                            .price(p.getPrice())
                            .mainImageUrl("")
                            .build())
                    .collect(Collectors.toList()));
            return voPage;
        }

        Page<Product> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (request.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, request.getCategoryId());
        }
        wrapper.eq(Product::getStatus, ProductStatus.ONLINE.getCode())
                .orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(page, wrapper);

        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream()
                .map(p -> ProductVO.builder()
                        .id(p.getId())
                        .title(p.getName())
                        .price(p.getPrice())
                        .mainImageUrl("")
                        .build())
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public List<SearchHistoryVO> getMySearchHistory(Integer limit) {
        Long userId;
        try {
            userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        } catch (Exception e) {
            log.warn("获取搜索历史失败：无法获取当前用户ID", e);
            return List.of();
        }

        int lim = limit != null ? limit : 20;

        // 先从 Redis 获取
        String key = ProductConstants.SEARCH_HISTORY_KEY_PREFIX + userId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, lim - 1);

        if (history != null && !history.isEmpty()) {
            // 去重并保持顺序
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

        // 回退到数据库
        List<SearchHistory> histories = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getSearchTime)
                        .last("LIMIT " + lim)); // 安全：lim 是方法参数，已在调用处限制范围

        return histories.stream()
                .map(h -> SearchHistoryVO.builder()
                        .id(h.getId())
                        .keyword(h.getKeyword())
                        .createTime(h.getSearchTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearMySearchHistory() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        // 清理 Redis
        redisTemplate.delete(ProductConstants.SEARCH_HISTORY_KEY_PREFIX + userId);

        // 清理数据库
        searchHistoryMapper.delete(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId));
    }

    @Override
    @Transactional
    public void deleteSearchHistory(Long historyId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        SearchHistory history = searchHistoryMapper.selectById(historyId);
        BizRequire.notNull(history, "搜索记录不存在");
        BizRequire.isTrue(history.getUserId().equals(userId), "无权操作此搜索记录");
        searchHistoryMapper.deleteById(historyId);
    }

    @Override
    public List<HotKeywordVO> getHotKeywords(Integer limit) {
        int lim = limit != null ? limit : 10;

        // 先从 Redis ZSet 获取
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

        // 回退到数据库
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
    public List<String> getSearchSuggestions(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // 从 Redis ZSet 中模糊匹配
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

        // 回退到数据库
        List<HotKeyword> keywords = hotKeywordMapper.selectList(
                new LambdaQueryWrapper<HotKeyword>()
                        .like(HotKeyword::getKeyword, keyword)
                        .orderByDesc(HotKeyword::getSearchCount)
                        .last("LIMIT " + (limit != null ? limit : 10)));

        return keywords.stream()
                .map(HotKeyword::getKeyword)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String trimmedKeyword = keyword.trim();

        Long userId;
        try {
            userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        } catch (Exception e) {
            log.warn("记录搜索历史失败：无法获取当前用户ID", e);
            return;
        }

        // Redis 热搜词统计（ZINCRBY 原子递增）
        redisTemplate.opsForZSet().incrementScore(ProductConstants.HOT_KEYWORD_ZSET_KEY, trimmedKeyword, 1);

        // Redis 搜索历史（List 存储，最新在左侧）
        String historyKey = ProductConstants.SEARCH_HISTORY_KEY_PREFIX + userId;
        Boolean keyExists = redisTemplate.hasKey(historyKey);
        redisTemplate.opsForList().leftPush(historyKey, trimmedKeyword);
        redisTemplate.opsForList().trim(historyKey, 0, ProductConstants.HOT_KEYWORD_LIMIT - 1);
        if (Boolean.FALSE.equals(keyExists)) {
            redisTemplate.expire(historyKey, Duration.ofDays(ProductConstants.SEARCH_HISTORY_EXPIRE_DAYS));
        }

        // 异步持久化到数据库（保留原有逻辑作为兜底）
        persistSearchHistory(userId, trimmedKeyword);
    }

    /**
     * 持久化搜索记录到数据库
     */
    private void persistSearchHistory(Long userId, String keyword) {
        // 保存搜索历史
        SearchHistory history = SearchHistory.builder()
                .userId(userId)
                .keyword(keyword)
                .searchTime(LocalDateTime.now())
                .build();
        searchHistoryMapper.insert(history);

        // 更新热词统计
        int updated = hotKeywordMapper.incrementSearchCount(keyword);
        if (updated == 0) {
            HotKeyword hotKeyword = HotKeyword.builder()
                    .keyword(keyword)
                    .searchCount(1)
                    .lastSearchTime(LocalDateTime.now())
                    .build();
            hotKeywordMapper.insert(hotKeyword);
        }
    }

    /**
     * 计算热度等级
     */
    private Integer calculateHotLevel(int searchCount) {
        if (searchCount >= ProductConstants.HOT_LEVEL_5_THRESHOLD) return 5;
        if (searchCount >= ProductConstants.HOT_LEVEL_4_THRESHOLD) return 4;
        if (searchCount >= ProductConstants.HOT_LEVEL_3_THRESHOLD) return 3;
        if (searchCount >= ProductConstants.HOT_LEVEL_2_THRESHOLD) return 2;
        if (searchCount >= ProductConstants.HOT_LEVEL_1_THRESHOLD) return 1;
        return 0;
    }

    /**
     * 每 30 分钟同步 Redis 热搜词统计到数据库
     */
    @Scheduled(cron = ProductConstants.CRON_SYNC_HOT_KEYWORDS)
    public void syncHotKeywordsToDatabase() {
        try {
            Set<Object> allKeywords = redisTemplate.opsForZSet()
                    .range(ProductConstants.HOT_KEYWORD_ZSET_KEY, 0, -1);

            if (allKeywords == null || allKeywords.isEmpty()) {
                return;
            }

            List<HotKeyword> keywordsToSync = new ArrayList<>();
            for (Object keywordObj : allKeywords) {
                String keyword = keywordObj.toString();
                Double score = redisTemplate.opsForZSet().score(ProductConstants.HOT_KEYWORD_ZSET_KEY, keyword);
                if (score == null) continue;

                keywordsToSync.add(HotKeyword.builder()
                        .keyword(keyword)
                        .searchCount(score.intValue())
                        .lastSearchTime(LocalDateTime.now())
                        .build());
            }

            if (!keywordsToSync.isEmpty()) {
                hotKeywordMapper.batchInsertOrUpdate(keywordsToSync);
                log.info("Hot keywords synced to database: {} keywords", keywordsToSync.size());
            }
        } catch (Exception e) {
            log.error("Failed to sync hot keywords to database", e);
        }
    }

    /**
     * 每天凌晨 2 点清理过期的搜索历史（超过 30 天）
     */
    @Scheduled(cron = ProductConstants.CRON_CLEANUP_SEARCH_HISTORY)
    public void cleanupExpiredSearchHistory() {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(30);
            int deletedCount = searchHistoryMapper.delete(
                    new LambdaQueryWrapper<SearchHistory>()
                            .lt(SearchHistory::getSearchTime, expireDate));
            log.info("Cleaned up {} expired search history records", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup expired search history", e);
        }
    }
}
