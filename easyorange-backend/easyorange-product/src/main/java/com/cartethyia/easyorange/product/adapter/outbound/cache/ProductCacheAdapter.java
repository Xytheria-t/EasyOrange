package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheAdapter implements ProductCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_INFO_KEY = "eo:product:info:";
    private static final long PRODUCT_CACHE_EXPIRE_HOURS = 24;

    @Override
    public ProductVO getProductCache(Long productId) {
        if (productId == null) {
            return null;
        }
        String key = PRODUCT_INFO_KEY + productId;
        try {
            Object cacheValue = redisTemplate.opsForValue().get(key);
            if (cacheValue instanceof ProductVO productVO) {
                return productVO;
            }
        } catch (Exception e) {
            log.error("获取商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
        return null;
    }

    @Override
    public void setProductCache(Long productId, ProductVO productVO) {
        if (productId == null || productVO == null) {
            return;
        }
        String key = PRODUCT_INFO_KEY + productId;
        try {
            redisTemplate.opsForValue().set(key, productVO, PRODUCT_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("设置商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
    }

    @Override
    public void evictProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        String key = PRODUCT_INFO_KEY + productId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
    }

    @Override
    public void evictProductListCache(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        String key = "eo:product:list:" + categoryId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除商品列表缓存失败：categoryId={}, error={}", categoryId, e.getMessage());
        }
    }
}
