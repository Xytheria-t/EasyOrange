package com.cartethyia.easyorange.product.adapter.outbound.persistence.search;

import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.product.adapter.outbound.cache.ProductCacheConstant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热词计数写入 — Redis zset 实时计数 + 缓冲批量 upsert 落 {@code eo_hot_keyword}。
 * <p>
 * 读侧（{@code findHotKeywords} / {@code findSearchSuggestions}）先读 zset、未命中才回落 DB，
 * 所以 zset 是榜单的实时口径，DB 表是重启后不至于清零的兜底快照。
 * <p>
 * 落在 adapter 层而非 application：落库要直注 {@link HotKeywordMapper}，放到上层就是
 * 「application 反向依赖 adapter」——那条债已被 ArchUnit 规则 7 冻结，新增违规会让门禁失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotKeywordBufferAdapter {

    private final HotKeywordMapper hotKeywordMapper;
    private final RedisTemplate<Object, Object> redisTemplate;

    /** 本次刷入前各热词的新增次数；按词聚合，同一词被搜 N 次只落一行。 */
    private final ConcurrentHashMap<String, AtomicInteger> pending = new ConcurrentHashMap<>();

    /**
     * 记一次搜索热词。
     * <p>
     * 永不抛：热词是非关键路径的旁挂增强，Redis 抖动不该让「记录搜索历史」整条写链路失败
     * （调用方同样兜了 try/catch，这里是第二层保险 —— zset 失败仍要把计数留给 DB 兜底）。
     */
    public void record(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String normalized = keyword.trim();
        try {
            redisTemplate.opsForZSet().incrementScore(ProductCacheConstant.HOT_KEYWORD_ZSET_KEY, normalized, 1);
        } catch (Exception e) {
            log.warn("action=hotKeywordZsetFailed keyword={}", normalized, e);
        }
        pending.computeIfAbsent(normalized, k -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * 定时把增量刷进库，并裁掉 zset 低分长尾。
     * <p>
     * 刷入失败直接丢弃本轮增量、不回插：热词计数是「多搜几次多几分」的统计量，
     * 丢一轮只影响榜单排序，不像搜索历史那样需要保证不丢。
     */
    @Scheduled(fixedRate = 5000)
    public void flush() {
        trimZsetTail();
        if (pending.isEmpty()) {
            return;
        }

        var now = LocalDateTime.now();
        var batch = new ArrayList<HotKeywordDO>(pending.size());
        for (Map.Entry<String, AtomicInteger> entry : pending.entrySet()) {
            batch.add(HotKeywordDO.builder()
                    .id(UuidV7.generateId())
                    .keyword(entry.getKey())
                    .searchCount(entry.getValue().get())
                    .lastSearchTime(now)
                    .build());
        }
        pending.clear();

        try {
            hotKeywordMapper.batchInsertOrUpdate(batch);
            log.debug("action=hotKeywordFlushDone size={}", batch.size());
        } catch (Exception e) {
            log.error("action=hotKeywordFlushFailed size={}", batch.size(), e);
        }
    }

    /** zset 只留分数最高的 {@link ProductCacheConstant#HOT_KEYWORD_MAX_SIZE} 个词，长尾不占内存也不进建议。 */
    private void trimZsetTail() {
        try {
            redisTemplate
                    .opsForZSet()
                    .removeRange(
                            ProductCacheConstant.HOT_KEYWORD_ZSET_KEY,
                            0,
                            -ProductCacheConstant.HOT_KEYWORD_MAX_SIZE - 1L);
        } catch (Exception e) {
            log.warn("action=hotKeywordZsetTrimFailed", e);
        }
    }

    /** 供测试断言缓冲深度。 */
    public int pendingSize() {
        return pending.size();
    }
}
