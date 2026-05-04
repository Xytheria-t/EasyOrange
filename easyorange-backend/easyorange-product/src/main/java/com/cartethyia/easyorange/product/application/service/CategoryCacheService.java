package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.domain.constant.ProductConstant;
import com.cartethyia.easyorange.product.domain.port.CategoryCachePort;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CategoryCacheService implements CategoryCachePort {

    private final CategoryQueryRepository categoryQueryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Cache<String, List<CategoryDO>> localCache;

    private static final String CACHE_KEY_LEVEL = ProductConstant.CATEGORY_LIST_KEY + ":level:";
    private static final String CACHE_KEY_PARENT = ProductConstant.CATEGORY_LIST_KEY + ":parent:";
    private static final String CACHE_KEY_ID = ProductConstant.CATEGORY_LIST_KEY + ":id:";
    private static final long REDIS_EXPIRE_MINUTES = 120;
    private static final int LOCAL_CACHE_MAX_SIZE = 100;
    private static final long LOCAL_CACHE_EXPIRE_MINUTES = 30;

    public CategoryCacheService(CategoryQueryRepository categoryQueryRepository,
                                RedisTemplate<String, Object> redisTemplate) {
        this.categoryQueryRepository = categoryQueryRepository;
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(LOCAL_CACHE_MAX_SIZE)
                .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public List<CategoryDO> getCategoriesByLevel(Integer level) {
        String cacheKey = CACHE_KEY_LEVEL + level;

        List<CategoryDO> cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            Object redisRaw = redisTemplate.opsForValue().get(cacheKey);
            if (redisRaw instanceof List<?> rawList && !rawList.isEmpty() && rawList.getFirst() instanceof CategoryDO) {
                @SuppressWarnings("unchecked")
                List<CategoryDO> redisCached = (List<CategoryDO>) rawList;
                localCache.put(cacheKey, redisCached);
                return redisCached;
            }
        } catch (Exception e) {
            log.warn("读取分类Redis缓存失败: level={}, error={}", level, e.getMessage());
        }

        List<CategoryDO> categories = categoryQueryRepository.findByLevel(level);
        if (categories == null) {
            categories = Collections.emptyList();
        }

        localCache.put(cacheKey, categories);
        try {
            redisTemplate.opsForValue().set(cacheKey, categories, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("设置分类Redis缓存失败: level={}, error={}", level, e.getMessage());
        }

        return categories;
    }

    @Override
    public List<CategoryDO> getCategoriesByParentId(Long parentId) {
        String cacheKey = CACHE_KEY_PARENT + parentId;

        List<CategoryDO> cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            Object redisRaw = redisTemplate.opsForValue().get(cacheKey);
            if (redisRaw instanceof List<?> rawList && !rawList.isEmpty() && rawList.getFirst() instanceof CategoryDO) {
                @SuppressWarnings("unchecked")
                List<CategoryDO> redisCached = (List<CategoryDO>) rawList;
                localCache.put(cacheKey, redisCached);
                return redisCached;
            }
        } catch (Exception e) {
            log.warn("读取分类Redis缓存失败: parentId={}, error={}", parentId, e.getMessage());
        }

        List<CategoryDO> categories = categoryQueryRepository.findByParentId(parentId);
        if (categories == null) {
            categories = Collections.emptyList();
        }

        localCache.put(cacheKey, categories);
        try {
            redisTemplate.opsForValue().set(cacheKey, categories, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("设置分类Redis缓存失败: parentId={}, error={}", parentId, e.getMessage());
        }

        return categories;
    }

    @Override
    public Optional<CategoryDO> getCategoryById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        String cacheKey = CACHE_KEY_ID + id;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof CategoryDO categoryDO) {
                return Optional.of(categoryDO);
            }
        } catch (Exception e) {
            log.warn("读取分类Redis缓存失败: id={}, error={}", id, e.getMessage());
        }

        List<CategoryDO> allCategories = categoryQueryRepository.findByIds(List.of(id));
        if (allCategories != null && !allCategories.isEmpty()) {
            CategoryDO category = allCategories.getFirst();
            try {
                redisTemplate.opsForValue().set(cacheKey, category, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("设置分类Redis缓存失败: id={}, error={}", id, e.getMessage());
            }
            return Optional.of(category);
        }

        return Optional.empty();
    }

    @Override
    public void evictAll() {
        localCache.invalidateAll();
        try {
            var keys = redisTemplate.keys(ProductConstant.CATEGORY_LIST_KEY + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清除分类Redis缓存失败: error={}", e.getMessage());
        }
    }

    @Override
    public void evictByLevel(Integer level) {
        String cacheKey = CACHE_KEY_LEVEL + level;
        localCache.invalidate(cacheKey);
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("清除分类Redis缓存失败: level={}, error={}", level, e.getMessage());
        }
    }

    @Override
    public void evictByParentId(Long parentId) {
        String cacheKey = CACHE_KEY_PARENT + parentId;
        localCache.invalidate(cacheKey);
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("清除分类Redis缓存失败: parentId={}, error={}", parentId, e.getMessage());
        }
    }
}
