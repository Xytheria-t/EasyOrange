package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.product.application.port.cache.ViewCountPort;
import com.cartethyia.easyorange.product.domain.valueobject.ViewCountEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** 浏览量计数适配器 — Redis hash 缓冲（key: {@value #VIEW_COUNT_KEY}）。 */
@Slf4j
@Component
public class RedisViewCountCacheAdapter implements ViewCountPort {

    private static final String VIEW_COUNT_KEY = "eo:product:views:pending";

    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisViewCountCacheAdapter(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void increment(String productId) {
        redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, productId, 1);
    }

    @Override
    public List<ViewCountEntry> findAllPending() {
        Map<Object, Object> pending = redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
        var entries = new ArrayList<ViewCountEntry>(pending.size());
        for (Map.Entry<Object, Object> entry : pending.entrySet()) {
            try {
                entries.add(new ViewCountEntry(
                        entry.getKey().toString(),
                        Integer.parseInt(entry.getValue().toString())));
            } catch (NumberFormatException e) {
                // 缓冲只写入整数（increment），解析失败说明数据被外部写坏——丢弃并清出缓冲，避免永久滞留。
                log.warn("invalid view count, drop key={}, value={}", entry.getKey(), entry.getValue());
                redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, entry.getKey());
            }
        }
        return entries;
    }

    @Override
    public void removePending(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, productIds.toArray());
    }
}
