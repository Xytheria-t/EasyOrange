package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountBatchProcessor {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final ProductMapper productMapper;

    private static final String VIEW_COUNT_KEY = "eo:product:views:pending";

    public void flush() {
        // Step 1: Read all pending views from Redis (non-transactional)
        var pendingViews = redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
        if (pendingViews.isEmpty()) return;

        var entries = new ArrayList<ProductMapper.ViewCountEntry>();
        for (Map.Entry<Object, Object> entry : pendingViews.entrySet()) {
            try {
                var count = Integer.parseInt(entry.getValue().toString());
                entries.add(new ProductMapper.ViewCountEntry(entry.getKey().toString(), count));
            } catch (NumberFormatException e) {
                log.warn("parse view count failed: key={}, value={}", entry.getKey(), entry.getValue());
            }
        }

        if (entries.isEmpty()) return;

        // Step 2: Batch update DB (transactional — Redis data is preserved if this fails)
        doBatchUpdate(entries);

        // Step 3: Clean up Redis (non-transactional, best-effort)
        try {
            redisTemplate
                    .opsForHash()
                    .delete(VIEW_COUNT_KEY, pendingViews.keySet().toArray());
        } catch (Exception e) {
            log.error("action=cleanupViewCountCacheFailed entries={}", entries.size(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    void doBatchUpdate(List<ProductMapper.ViewCountEntry> entries) {
        productMapper.batchAddViewCounts(entries);
        log.debug("batch update view count done: processed={}", entries.size());
    }
}
