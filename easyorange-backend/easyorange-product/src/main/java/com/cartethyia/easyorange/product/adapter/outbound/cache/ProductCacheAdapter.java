package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.bloom.RedisBitmapBloomFilter;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheAdapter implements ProductCachePort<ProductVO> {

    private final MultiLevelCache multiLevelCache;
    private final RedisBitmapBloomFilter bloomFilter;

    @Override
    public ProductVO getProductCache(Long productId) {
        if (productId == null) {
            return null;
        }

        String idStr = productId.toString();
        if (!bloomFilter.mightContain(ProductCacheConstant.PRODUCT_BLOOM_KEY, idStr)) {
            log.debug("action=bloom_filter_miss productId={}", productId);
            return null;
        }

        try {
            return multiLevelCache.get(ProductCacheConstant.infoKey(productId), ProductVO.class, () -> null);
        } catch (Exception e) {
            log.warn("获取商品缓存失败: productId={}", productId, e);
            return null;
        }
    }

    @Override
    public void setProductCache(Long productId, ProductVO productVO) {
        if (productId == null || productVO == null) {
            return;
        }
        try {
            addToBloomFilter(productId);
            multiLevelCache.put(ProductCacheConstant.infoKey(productId), productVO);
        } catch (Exception e) {
            log.warn("设置商品缓存失败: productId={}", productId, e);
        }
    }

    @Override
    public void evictProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            multiLevelCache.evict(ProductCacheConstant.infoKey(productId));
        } catch (Exception e) {
            log.warn("删除商品缓存失败: productId={}", productId, e);
        }
    }

    @Override
    public void evictProductListCache(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        try {
            multiLevelCache.evictL2(ProductCacheConstant.listKey(categoryId));
        } catch (Exception e) {
            log.warn("删除商品列表缓存失败: categoryId={}", categoryId, e);
        }
    }

    public void addToBloomFilter(Long productId) {
        if (productId == null) {
            return;
        }
        bloomFilter.put(ProductCacheConstant.PRODUCT_BLOOM_KEY, productId.toString());
    }
}