package com.cartethyia.easyorange.order.adapter.outbound.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RedisOrderCacheAdapter implements OrderCachePort<OrderVO> {

    private static final String KEY_PREFIX = "eo:order:list:";
    private static final long TTL_MINUTES = 30;

    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PageResult<OrderVO>> getOrderList(String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank()) return Optional.empty();
        try {
            var cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PageResult<?> result) {
                return Optional.of((PageResult<OrderVO>) result);
            }
        } catch (Exception e) {
            log.warn("Failed to get order list cache: key={}", cacheKey, e);
        }
        return Optional.empty();
    }

    @Override
    public void putOrderList(String cacheKey, PageResult<OrderVO> pageResult) {
        if (cacheKey == null || pageResult == null) return;
        try {
            redisTemplate.opsForValue().set(cacheKey, pageResult, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to set order list cache: key={}", cacheKey, e);
        }
    }

    @Override
    public void evictOrderCache(String buyerId, String sellerId) {
        if (buyerId != null) evictByPattern(KEY_PREFIX + buyerId + ":*");
        if (sellerId != null) evictByPattern(KEY_PREFIX + sellerId + ":*");
    }

    @Override
    public String buildOrderListKey(String userId, String status, Integer pageNum, Integer pageSize) {
        if (userId == null) return null;
        var key =
                new StringBuilder(KEY_PREFIX).append(userId).append(":status:").append(status != null ? status : "all");
        if (pageNum != null && pageSize != null) {
            key.append(":page:").append(pageNum).append(":size:").append(pageSize);
        }
        return key.toString();
    }

    private void evictByPattern(String pattern) {
        // SCAN 游标遍历替代 KEYS 全键扫描，避免阻塞生产 Redis；游标式遍历期间新增的 key 留给 TTL 兜底
        try (Cursor<Object> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            List<Object> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to evict by pattern: pattern={}", pattern, e);
        }
    }
}
