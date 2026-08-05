package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.port.cache.ProductCachePort;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ProductCacheAdapter implements ProductCachePort, ProductCacheEvictionPort {

    private final MultiLevelCache multiLevelCache;

    @Override
    public Optional<ProductVO> getProductCache(String productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return cached(ProductCacheConstant.infoKey(productId));
    }

    @Override
    public void setProductCache(String productId, ProductVO productVO) {
        if (productId == null || productVO == null) {
            return;
        }
        put(ProductCacheConstant.infoKey(productId), productVO);
    }

    @Override
    public void evictProductCache(String productId) {
        if (productId == null) {
            return;
        }
        evict(ProductCacheConstant.infoKey(productId));
    }

    @Override
    public void evictProductListCache(String categoryId) {
        if (categoryId == null) {
            return;
        }
        evict(ProductCacheConstant.listKey(categoryId));
    }

    // ── Private helpers ──

    private Optional<ProductVO> cached(String key) {
        try {
            return Optional.ofNullable(multiLevelCache.get(key, ProductVO.class, () -> null));
        } catch (Exception e) {
            log.warn("action=cacheGetFailed key={}", key, e);
            return Optional.empty();
        }
    }

    private void put(String key, ProductVO value) {
        try {
            multiLevelCache.put(key, value);
        } catch (Exception e) {
            log.warn("action=cachePutFailed key={}", key, e);
        }
    }

    private void evict(String key) {
        try {
            multiLevelCache.evict(key);
        } catch (Exception e) {
            log.warn("action=cacheEvictFailed key={}", key, e);
        }
    }
}
