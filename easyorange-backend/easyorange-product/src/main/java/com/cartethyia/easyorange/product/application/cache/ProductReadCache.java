package com.cartethyia.easyorange.product.application.cache;

import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductReadCache {

    private final RedisCache redisCache;

    public Optional<ProductVO> getDetail(Long productId) {
        String key = CacheConstants.Product.infoKey(productId);
        return Optional.ofNullable(redisCache.get(key, ProductVO.class));
    }

    public void putDetail(Long productId, ProductVO productVO) {
        String key = CacheConstants.Product.infoKey(productId);
        redisCache.set(key, productVO, CacheConstants.Product.INFO_EXPIRE_TIME, java.util.concurrent.TimeUnit.MINUTES);
    }

    public void evict(Long productId) {
        String key = CacheConstants.Product.infoKey(productId);
        redisCache.delete(key);
    }

    public void evictAll(Long... productIds) {
        BizRequire.notEmpty(productIds, "商品 ID 数组不能为空");
        for (Long productId : productIds) {
            evict(productId);
        }
    }
}
