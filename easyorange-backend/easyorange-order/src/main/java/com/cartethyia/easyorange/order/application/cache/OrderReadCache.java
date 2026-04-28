package com.cartethyia.easyorange.order.application.cache;

import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.order.constant.OrderConstant;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderReadCache {

    private final RedisCache redisCache;

    public Optional<OrderVO> getDetail(Long orderId) {
        String key = OrderConstant.orderDetailKey(orderId);
        return Optional.ofNullable(redisCache.get(key, OrderVO.class));
    }

    public void putDetail(Long orderId, OrderVO orderVO) {
        String key = OrderConstant.orderDetailKey(orderId);
        redisCache.set(key, orderVO, OrderConstant.ORDER_DETAIL_EXPIRE_TIME, java.util.concurrent.TimeUnit.MINUTES);
    }

    public void evict(Long orderId) {
        String key = OrderConstant.orderDetailKey(orderId);
        redisCache.delete(key);
    }
}
