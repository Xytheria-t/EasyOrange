package com.cartethyia.easyorange.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewCountAppService {

    private final RedisTemplate<Object, Object> redisTemplate;

    private static final String VIEW_COUNT_KEY = "eo:product:views:pending";

    public void incrementViewCount(String productId) {
        if (productId == null) return;
        try {
            redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, productId, 1);
        } catch (Exception e) {
            log.warn("记录浏览量失败: productId={}", productId, e);
        }
    }
}
