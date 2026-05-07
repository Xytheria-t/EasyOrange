package com.cartethyia.easyorange.order.infrastructure.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.response.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_LIST_CACHE_KEY_PREFIX = "eo:order:list:";
    private static final String ORDER_DETAIL_CACHE_KEY_PREFIX = "eo:order:detail:";
    private static final long ORDER_LIST_CACHE_EXPIRE_MINUTES = 30;
    private static final long ORDER_DETAIL_CACHE_EXPIRE_MINUTES = 60;
    private static final int MAX_RETRY_COUNT = 2;

    @SuppressWarnings("unchecked")
    public Optional<PageResult<OrderVO>> getOrderListCache(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return Optional.empty();
        }
        
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
                if (cacheValue instanceof PageResult pageResult) {
                    return Optional.of(pageResult);
                }
                return Optional.empty();
            } catch (Exception e) {
                log.warn("获取订单列表缓存失败（重试 {}/{}）：key={}, error={}", 
                    retry + 1, MAX_RETRY_COUNT, cacheKey, e.getMessage());
                if (retry == MAX_RETRY_COUNT - 1) {
                    log.error("获取订单列表缓存最终失败，启用降级策略：key={}", cacheKey);
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public void setOrderListCache(String cacheKey, PageResult<OrderVO> pageResult) {
        if (cacheKey == null || pageResult == null) {
            return;
        }
        
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                redisTemplate.opsForValue().set(cacheKey, pageResult, ORDER_LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                log.warn("设置订单列表缓存失败（重试 {}/{}）：key={}, error={}", 
                    retry + 1, MAX_RETRY_COUNT, cacheKey, e.getMessage());
                if (retry == MAX_RETRY_COUNT - 1) {
                    log.error("设置订单列表缓存最终失败，放弃缓存：key={}", cacheKey);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<OrderVO> getOrderDetailCache(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }

        String cacheKey = buildOrderDetailKey(orderId);
        
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
                if (cacheValue instanceof OrderVO orderVO) {
                    return Optional.of(orderVO);
                }
                return Optional.empty();
            } catch (Exception e) {
                log.warn("获取订单详情缓存失败（重试 {}/{}）：key={}, error={}", 
                    retry + 1, MAX_RETRY_COUNT, cacheKey, e.getMessage());
                if (retry == MAX_RETRY_COUNT - 1) {
                    log.error("获取订单详情缓存最终失败，启用降级策略：key={}", cacheKey);
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public void setOrderDetailCache(OrderVO orderVO) {
        if (orderVO == null || orderVO.getId() == null) {
            return;
        }

        String cacheKey = buildOrderDetailKey(orderVO.getId());
        
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                redisTemplate.opsForValue().set(cacheKey, orderVO, ORDER_DETAIL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                log.warn("设置订单详情缓存失败（重试 {}/{}）：key={}, error={}", 
                    retry + 1, MAX_RETRY_COUNT, cacheKey, e.getMessage());
                if (retry == MAX_RETRY_COUNT - 1) {
                    log.error("设置订单详情缓存最终失败，放弃缓存：key={}", cacheKey);
                }
            }
        }
    }

    public PageResult<OrderVO> getOrLoadOrderList(
        String cacheKey, 
        Supplier<PageResult<OrderVO>> loader
    ) {
        return getOrderListCache(cacheKey)
            .orElseGet(() -> {
                PageResult<OrderVO> result = loader.get();
                setOrderListCache(cacheKey, result);
                return result;
            });
    }

    public OrderVO getOrLoadOrderDetail(
        Long orderId,
        Supplier<OrderVO> loader
    ) {
        return getOrderDetailCache(orderId)
            .orElseGet(() -> {
                OrderVO result = loader.get();
                setOrderDetailCache(result);
                return result;
            });
    }

    public void deleteOrderListCache(String cacheKey) {
        if (cacheKey == null) {
            return;
        }
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.error("删除订单列表缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
    }

    public void deleteOrderDetailCache(Long orderId) {
        if (orderId == null) {
            return;
        }

        String cacheKey = buildOrderDetailKey(orderId);
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.error("删除订单详情缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
    }

    public void deleteBuyerOrderCache(Long buyerId) {
        if (buyerId == null) {
            return;
        }
        
        deleteOrderCacheByRole(buyerId, "buyer");
    }

    public void deleteSellerOrderCache(Long sellerId) {
        if (sellerId == null) {
            return;
        }
        
        deleteOrderCacheByRole(sellerId, "seller");
    }

    public void deleteOrderCache(Long buyerId, Long sellerId) {
        if (buyerId != null) {
            deleteBuyerOrderCache(buyerId);
        }
        if (sellerId != null) {
            deleteSellerOrderCache(sellerId);
        }
    }

    private void deleteOrderCacheByRole(Long roleId, String roleType) {
        String pattern = ORDER_LIST_CACHE_KEY_PREFIX + roleId + ":*";
        
        try {
            Collection<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("清除用户订单缓存失败：roleId={}, error={}", roleId, e.getMessage());
            for (OrderStatus status : OrderStatus.values()) {
                String cacheKey = buildOrderListKey(roleId, status.getCode(), 1, 10);
                deleteOrderListCache(cacheKey);
            }
            deleteOrderListCache(buildOrderListKey(roleId, null, 1, 10));
        }
    }

    public String buildOrderListKey(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            return null;
        }
        
        StringBuilder keyBuilder = new StringBuilder(ORDER_LIST_CACHE_KEY_PREFIX);
        keyBuilder.append(userId);
        
        if (status != null) {
            keyBuilder.append(":status:").append(status);
        } else {
            keyBuilder.append(":status:all");
        }
        
        if (pageNum != null && pageSize != null) {
            keyBuilder.append(":page:").append(pageNum).append(":size:").append(pageSize);
        }
        
        return keyBuilder.toString();
    }

    public String buildOrderListKey(Long userId, Integer status) {
        return buildOrderListKey(userId, status, 1, 10);
    }

    private String buildOrderDetailKey(Long orderId) {
        return ORDER_DETAIL_CACHE_KEY_PREFIX + orderId;
    }

    public void warmUpCache(Long userId) {
        try {
            for (int status = 0; status <= 5; status++) {
                String cacheKey = buildOrderListKey(userId, status, 1, 10);
                redisTemplate.opsForValue().set(cacheKey, PageResult.empty(1, 10), ORDER_LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("用户订单缓存预热失败：userId={}", userId, e);
        }
    }
}
