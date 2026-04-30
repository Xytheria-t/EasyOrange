package com.cartethyia.easyorange.product.application.cache;

import com.cartethyia.easyorange.product.domain.constant.ProductConstant;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.repository.ProductCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheService implements ProductCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long PRODUCT_CACHE_EXPIRE_HOURS = 24;

    @Override
    public ProductVO getProductCache(Long productId) {
        if (productId == null) {
            return null;
        }
        String key = ProductConstant.infoKey(productId);
        try {
            Object cacheValue = redisTemplate.opsForValue().get(key);
            if (cacheValue instanceof ProductVO productVO) {
                log.debug("命中商品缓存：productId={}", productId);
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
        String key = ProductConstant.infoKey(productId);
        try {
            redisTemplate.opsForValue().set(key, productVO, PRODUCT_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("设置商品缓存：productId={}", productId);
        } catch (Exception e) {
            log.error("设置商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
    }

    @Override
    public void evictProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        String key = ProductConstant.infoKey(productId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("删除商品缓存：productId={}", productId);
            }
        } catch (Exception e) {
            log.error("删除商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
    }

    @Override
    public void evictProductListCache(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        String key = ProductConstant.listKey(categoryId);
        try {
            redisTemplate.delete(key);
            log.debug("删除商品列表缓存：categoryId={}", categoryId);
        } catch (Exception e) {
            log.error("删除商品列表缓存失败：categoryId={}, error={}", categoryId, e.getMessage());
        }
    }

    public void deleteProductBatchCache(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        for (Long productId : productIds) {
            evictProductCache(productId);
        }
    }

    public Boolean hasProductCache(Long productId) {
        if (productId == null) {
            return false;
        }
        String key = ProductConstant.infoKey(productId);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查商品缓存失败：productId={}, error={}", productId, e.getMessage());
            return false;
        }
    }
}
