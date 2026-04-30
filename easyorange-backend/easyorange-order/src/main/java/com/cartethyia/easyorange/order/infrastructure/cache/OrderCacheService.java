package com.cartethyia.easyorange.order.infrastructure.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.interfaces.dto.response.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_LIST_CACHE_KEY_PREFIX = "order:list:";
    private static final long ORDER_LIST_CACHE_EXPIRE_MINUTES = 30;

    @SuppressWarnings("unchecked")
    public PageResult<OrderVO> getOrderListCache(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return null;
        }
        
        try {
            Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
            if (cacheValue instanceof PageResult pageResult) {
                log.debug("命中订单列表缓存：key={}", cacheKey);
                return pageResult;
            }
        } catch (Exception e) {
            log.error("获取订单列表缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
        return null;
    }

    public void setOrderListCache(String cacheKey, PageResult<OrderVO> pageResult) {
        if (cacheKey == null || pageResult == null) {
            return;
        }
        
        try {
            redisTemplate.opsForValue().set(cacheKey, pageResult, ORDER_LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("设置订单列表缓存：key={}", cacheKey);
        } catch (Exception e) {
            log.error("设置订单列表缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
    }

    public void deleteOrderListCache(String cacheKey) {
        if (cacheKey == null) {
            return;
        }
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("删除订单列表缓存：key={}", cacheKey);
            }
        } catch (Exception e) {
            log.error("删除订单列表缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
    }

    public void deleteBuyerOrderCache(Long buyerId) {
        if (buyerId == null) {
            return;
        }
        
        deleteOrderCacheByRole(buyerId);
    }

    public void deleteSellerOrderCache(Long sellerId) {
        if (sellerId == null) {
            return;
        }
        
        deleteOrderCacheByRole(sellerId);
    }

    public void deleteOrderCache(Long buyerId, Long sellerId) {
        if (buyerId != null) {
            deleteBuyerOrderCache(buyerId);
        }
        if (sellerId != null) {
            deleteSellerOrderCache(sellerId);
        }
    }

    private void deleteOrderCacheByRole(Long roleId) {
        for (int status = 0; status <= 5; status++) {
            String cacheKey = buildOrderListKey(roleId, status);
            deleteOrderListCache(cacheKey);
        }
        
        deleteOrderListCache(buildOrderListKey(roleId, null));
    }

    public String buildOrderListKey(Long buyerId, Integer status) {
        if (buyerId == null) {
            return null;
        }
        if (status != null) {
            return ORDER_LIST_CACHE_KEY_PREFIX + buyerId + ":" + status;
        }
        return ORDER_LIST_CACHE_KEY_PREFIX + buyerId + ":all";
    }
}
