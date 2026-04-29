package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.constant.ProductConstant;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.dto.vo.HotKeywordVO;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.dto.vo.SearchHistoryVO;
import com.cartethyia.easyorange.product.entity.HotKeyword;
import com.cartethyia.easyorange.product.entity.SearchHistory;
import com.cartethyia.easyorange.product.mapper.HotKeywordMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProductQueryRepository productQueryRepository;
    private final SearchHistoryMapper searchHistoryMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<ProductVO> searchProducts(ProductSearchRequest request) {
        PageRequest normalized = request.normalized();
        return productQueryRepository.searchProducts(
                request.getKeyword(),
                request.getCategoryId(),
                null,
                normalized.getPageNum(),
                normalized.getPageSize()
        );
    }

    @Override
    public List<SearchHistoryVO> getMySearchHistory(Integer limit) {
        BizRequire.notNull(limit, "限制数量不能为空");
        BizRequire.positive(limit, "限制数量必须为正数");
        BizRequire.between(limit, 1, 100, "限制数量必须在 1-100 之间");
        
        Long userId;
        try {
            userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        } catch (Exception e) {
            log.warn("获取搜索历史失败：无法获取当前用户 ID", e);
            return List.of();
        }

        return productQueryRepository.findSearchHistoryByUserId(userId, limit);
    }

    @Override
    @Transactional
    public void clearMySearchHistory() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        redisTemplate.delete(ProductConstant.SEARCH_HISTORY_KEY_PREFIX + userId);

        searchHistoryMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId));
    }

    @Override
    @Transactional
    public void deleteSearchHistory(Long historyId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.notNull(historyId, "搜索记录 ID 不能为空");
        BizRequire.positive(historyId, "搜索记录 ID 必须为正数");
        
        SearchHistory history = searchHistoryMapper.selectById(historyId);
        BizRequire.notNull(history, "搜索记录不存在");
        BizRequire.eq(history.getUserId(), userId, "无权操作此搜索记录");
        searchHistoryMapper.deleteById(historyId);
    }

    @Override
    public List<HotKeywordVO> getHotKeywords(Integer limit) {
        BizRequire.notNull(limit, "限制数量不能为空");
        BizRequire.positive(limit, "限制数量必须为正数");
        BizRequire.between(limit, 1, 50, "限制数量必须在 1-50 之间");
        
        return productQueryRepository.findHotKeywords(limit);
    }

    @Override
    public List<String> getSearchSuggestions(String keyword, Integer limit) {
        BizRequire.notBlank(keyword, "搜索关键词不能为空");
        BizRequire.notNull(limit, "限制数量不能为空");
        BizRequire.positive(limit, "限制数量必须为正数");
        BizRequire.between(limit, 1, 20, "限制数量必须在 1-20 之间");
        BizRequire.requireFalse(keyword.length() > 100, "搜索关键词不能超过 100 个字符");
        
        return productQueryRepository.findSearchSuggestions(keyword, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String trimmedKeyword = keyword.trim();

        Long userId;
        try {
            userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        } catch (Exception e) {
            log.warn("记录搜索历史失败：无法获取当前用户 ID", e);
            return;
        }

        redisTemplate.opsForZSet().incrementScore(ProductConstant.HOT_KEYWORD_ZSET_KEY, trimmedKeyword, 1);

        String historyKey = ProductConstant.SEARCH_HISTORY_KEY_PREFIX + userId;
        Boolean keyExists = redisTemplate.hasKey(historyKey);
        redisTemplate.opsForList().leftPush(historyKey, trimmedKeyword);
        redisTemplate.opsForList().trim(historyKey, 0, ProductConstant.HOT_KEYWORD_LIMIT - 1);
        if (Boolean.FALSE.equals(keyExists)) {
            redisTemplate.expire(historyKey, Duration.ofDays(ProductConstant.SEARCH_HISTORY_EXPIRE_DAYS));
        }

        persistSearchHistory(userId, trimmedKeyword);
    }

    private void persistSearchHistory(Long userId, String keyword) {
        SearchHistory history = SearchHistory.builder()
                .userId(userId)
                .keyword(keyword)
                .searchTime(LocalDateTime.now())
                .build();
        searchHistoryMapper.insert(history);

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

    private Integer calculateHotLevel(int searchCount) {
        if (searchCount >= ProductConstant.HOT_LEVEL_5_THRESHOLD) return 5;
        if (searchCount >= ProductConstant.HOT_LEVEL_4_THRESHOLD) return 4;
        if (searchCount >= ProductConstant.HOT_LEVEL_3_THRESHOLD) return 3;
        if (searchCount >= ProductConstant.HOT_LEVEL_2_THRESHOLD) return 2;
        if (searchCount >= ProductConstant.HOT_LEVEL_1_THRESHOLD) return 1;
        return 0;
    }

    @Scheduled(cron = ProductConstant.CRON_SYNC_HOT_KEYWORDS)
    public void syncHotKeywordsToDatabase() {
        try {
            Set<Object> allKeywords = redisTemplate.opsForZSet()
                    .range(ProductConstant.HOT_KEYWORD_ZSET_KEY, 0, -1);

            if (allKeywords == null || allKeywords.isEmpty()) {
                return;
            }

            List<HotKeyword> keywordsToSync = new ArrayList<>();
            for (Object keywordObj : allKeywords) {
                String keyword = keywordObj.toString();
                Double score = redisTemplate.opsForZSet().score(ProductConstant.HOT_KEYWORD_ZSET_KEY, keyword);
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

    @Scheduled(cron = ProductConstant.CRON_CLEANUP_SEARCH_HISTORY)
    public void cleanupExpiredSearchHistory() {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(30);
            int deletedCount = searchHistoryMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SearchHistory>()
                            .lt(SearchHistory::getSearchTime, expireDate));
            log.info("Cleaned up {} expired search history records", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup expired search history", e);
        }
    }
}
