package com.cartethyia.easyorange.order.application.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单缓存服务
 * 
 * @author cartethyia
 * @date 2026/04/23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_LIST_CACHE_KEY_PREFIX = "order:list:";
    private static final long ORDER_LIST_CACHE_EXPIRE_MINUTES = 30;

    /**
     * 获取订单列表缓存
     *
     * @param cacheKey 缓存键（由买家 ID+ 状态等组成）
     * @return 缓存的订单列表，不存在返回 null
     */
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

    /**
     * 设置订单列表缓存
     *
     * @param cacheKey 缓存键
     * @param pageResult 订单列表数据
     */
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

    /**
     * 删除订单列表缓存
     *
     * @param cacheKey 缓存键
     */
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

    /**
     * 删除买家订单缓存（清除该买家的所有订单列表缓存）
     *
     * @param buyerId 买家 ID
     */
    public void deleteBuyerOrderCache(Long buyerId) {
        if (buyerId == null) {
            return;
        }
        
        // 删除所有买家的订单缓存（不同状态）
        for (int status = 0; status <= 5; status++) {
            String cacheKey = buildOrderListKey(buyerId, status);
            deleteOrderListCache(cacheKey);
        }
        
        // 删除不分状态的缓存
        deleteOrderListCache(buildOrderListKey(buyerId, null));
    }

    /**
     * 构建订单列表缓存键
     *
     * @param buyerId 买家 ID
     * @param status 订单状态（可选）
     * @return 缓存键
     */
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
