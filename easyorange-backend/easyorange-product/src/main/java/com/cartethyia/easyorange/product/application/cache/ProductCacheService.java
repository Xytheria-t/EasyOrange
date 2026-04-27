package com.cartethyia.easyorange.product.application.cache;

import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 商品缓存服务
 * 
 * @author cartethyia
 * @date 2026/04/23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final long PRODUCT_CACHE_EXPIRE_HOURS = 24;

    /**
     * 获取商品缓存
     *
     * @param productId 商品 ID
     * @return 缓存的商品信息，不存在返回 null
     */
    public ProductVO getProductCache(Long productId) {
        if (productId == null) {
            return null;
        }
        
        String key = buildProductKey(productId);
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

    /**
     * 设置商品缓存
     *
     * @param productId 商品 ID
     * @param productVO 商品信息
     */
    public void setProductCache(Long productId, ProductVO productVO) {
        if (productId == null || productVO == null) {
            return;
        }
        
        String key = buildProductKey(productId);
        try {
            redisTemplate.opsForValue().set(key, productVO, PRODUCT_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("设置商品缓存：productId={}", productId);
        } catch (Exception e) {
            log.error("设置商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
    }

    /**
     * 删除商品缓存
     *
     * @param productId 商品 ID
     */
    public void deleteProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        
        String key = buildProductKey(productId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("删除商品缓存：productId={}", productId);
            }
        } catch (Exception e) {
            log.error("删除商品缓存失败：productId={}, error={}", productId, e.getMessage());
        }
    }

    /**
     * 批量删除商品缓存
     *
     * @param productIds 商品 ID 列表
     */
    public void deleteProductBatchCache(java.util.List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        
        for (Long productId : productIds) {
            deleteProductCache(productId);
        }
    }

    /**
     * 检查商品缓存是否存在
     *
     * @param productId 商品 ID
     * @return 是否存在
     */
    public Boolean hasProductCache(Long productId) {
        if (productId == null) {
            return false;
        }
        
        String key = buildProductKey(productId);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查商品缓存失败：productId={}, error={}", productId, e.getMessage());
            return false;
        }
    }

    private String buildProductKey(Long productId) {
        return PRODUCT_CACHE_KEY_PREFIX + productId;
    }
}
