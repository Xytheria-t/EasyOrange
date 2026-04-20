package com.cartethyia.easyorange.order.application.cache;

import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderReadCache {

    private final RedisCache redisCache;

    private static final String ORDER_KEY_PREFIX = CacheConstants.APP_PREFIX + "order:";

    public Optional<OrderVO> getDetail(Long orderId) {
        String key = ORDER_KEY_PREFIX + "detail:" + orderId;
        return Optional.ofNullable(redisCache.get(key, OrderVO.class));
    }

    public void putDetail(Long orderId, OrderVO orderVO) {
        String key = ORDER_KEY_PREFIX + "detail:" + orderId;
        redisCache.set(key, orderVO, 30, java.util.concurrent.TimeUnit.MINUTES);
    }

    public void evict(Long orderId) {
        String key = ORDER_KEY_PREFIX + "detail:" + orderId;
        redisCache.delete(key);
    }
}
