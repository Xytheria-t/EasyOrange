package com.cartethyia.easyorange.order.adapter.outbound.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.response.OrderVO;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOrderCacheAdapter implements OrderCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_LIST_CACHE_KEY_PREFIX = "eo:order:list:";
    private static final String ORDER_DETAIL_CACHE_KEY_PREFIX = "eo:order:detail:";
    private static final long ORDER_LIST_CACHE_EXPIRE_MINUTES = 30;
    private static final long ORDER_DETAIL_CACHE_EXPIRE_MINUTES = 60;
    private static final int MAX_RETRY_COUNT = 2;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PageResult<OrderVO>> getOrderList(String cacheKey) {
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

    @Override
    public void putOrderList(String cacheKey, PageResult<OrderVO> pageResult) {
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

    @Override
    public void evictOrderList(String cacheKey) {
        if (cacheKey == null) {
            return;
        }

        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.error("删除订单列表缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<OrderVO> getOrderDetail(Long orderId) {
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

    @Override
    public void putOrderDetail(Long orderId, OrderVO orderVO) {
        if (orderId == null || orderVO == null) {
            return;
        }

        String cacheKey = buildOrderDetailKey(orderId);

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

    @Override
    public void evictOrderDetail(Long orderId) {
        if (orderId == null) {
            return;
        }

        String cacheKey = buildOrderDetailKey(orderId);

        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.error("删除订单详情缓存失败：key={}, error={}", cacheKey, e.getMessage());
        }
    }

    @Override
    public void evictBuyerOrders(Long buyerId) {
        if (buyerId == null) {
            return;
        }

        evictOrderCacheByRole(buyerId, "buyer");
    }

    @Override
    public void evictSellerOrders(Long sellerId) {
        if (sellerId == null) {
            return;
        }

        evictOrderCacheByRole(sellerId, "seller");
    }

    @Override
    public void evictOrderCache(Long buyerId, Long sellerId) {
        if (buyerId != null) {
            evictBuyerOrders(buyerId);
        }
        if (sellerId != null) {
            evictSellerOrders(sellerId);
        }
    }

    @Override
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

    @Override
    public String buildOrderListKey(Long userId, Integer status) {
        return buildOrderListKey(userId, status, 1, 10);
    }

    private void evictOrderCacheByRole(Long roleId, String roleType) {
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
                evictOrderList(cacheKey);
            }
            evictOrderList(buildOrderListKey(roleId, null, 1, 10));
        }
    }

    private String buildOrderDetailKey(Long orderId) {
        return ORDER_DETAIL_CACHE_KEY_PREFIX + orderId;
    }
}
