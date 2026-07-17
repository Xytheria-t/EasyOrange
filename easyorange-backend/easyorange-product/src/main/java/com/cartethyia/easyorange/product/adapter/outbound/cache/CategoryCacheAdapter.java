package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.cache.CacheUtils;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.port.CategoryCachePort;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分类缓存适配器
 *
 * 改进点：
 * 1. 将 broad catch 改为具体异常类型（RedisConnectionFailureException, QueryTimeoutException）
 * 2. 添加告警机制：ERROR 级别日志 + 失败计数器
 * 3. 添加简单熔断机制：连续失败达到阈值时跳过 Redis，直接降级到 DB
 * 4. 保持降级逻辑，确保 DB 查询可用
 */
@Slf4j
@Component
public class CategoryCacheAdapter implements CategoryCachePort<CategoryReadModel> {

    private final CategoryQueryRepository categoryQueryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, List<CategoryReadModel>> localCache;

    // 熔断状态管理
    private final AtomicInteger redisFailureCount = new AtomicInteger(0);
    private final AtomicBoolean circuitBreakerOpen = new AtomicBoolean(false);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    private static final String CACHE_KEY_PREFIX = "eo:category:list";
    private static final String CACHE_KEY_LEVEL = CACHE_KEY_PREFIX + ":level:";
    private static final String CACHE_KEY_PARENT = CACHE_KEY_PREFIX + ":parent:";
    private static final String CACHE_KEY_ID = CACHE_KEY_PREFIX + ":id:";
    private static final long REDIS_EXPIRE_MINUTES = 120;
    private static final int LOCAL_CACHE_MAX_SIZE = 100;
    private static final long LOCAL_CACHE_EXPIRE_MINUTES = 30;

    // 熔断配置
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5; // 连续失败 5 触发熔断
    private static final long CIRCUIT_BREAKER_RESET_INTERVAL_MS = 60_000; // 60 秒后重置熔断

    public CategoryCacheAdapter(CategoryQueryRepository categoryQueryRepository,
                                RedisTemplate<String, Object> redisTemplate) {
        this.categoryQueryRepository = categoryQueryRepository;
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(LOCAL_CACHE_MAX_SIZE)
                .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public List<CategoryReadModel> getCategoriesByLevel(Integer level) {
        String cacheKey = CACHE_KEY_LEVEL + level;

        List<CategoryReadModel> cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 检查熔断状态
        if (!shouldSkipRedis()) {
            try {
                Object redisRaw = redisTemplate.opsForValue().get(cacheKey);
                if (redisRaw instanceof List<?> rawList && !rawList.isEmpty() && rawList.getFirst() instanceof CategoryReadModel) {
                    @SuppressWarnings("unchecked")
                    List<CategoryReadModel> redisCached = (List<CategoryReadModel>) redisRaw;
                    localCache.put(cacheKey, redisCached);
                    recordRedisSuccess();
                    return redisCached;
                }
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("读取分类缓存", "level=" + level, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("读取分类缓存", "level=" + level, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("读取分类缓存", "level=" + level, e);
            }
        } else {
            log.warn("action=circuit_breaker_skip_redis, operation=读取分类缓存, key={}", cacheKey);
        }

        List<CategoryDO> categories = categoryQueryRepository.findByLevel(level);
        if (categories == null) {
            categories = List.of();
        }
        List<CategoryReadModel> models = categories.stream().map(this::toReadModel).toList();

        localCache.put(cacheKey, models);

        // 写入 Redis（仅在熔断未开启时）
        if (!shouldSkipRedis()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, models, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
                recordRedisSuccess();
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("设置分类缓存", "level=" + level, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("设置分类缓存", "level=" + level, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("设置分类缓存", "level=" + level, e);
            }
        }

        return models;
    }

    @Override
    public List<CategoryReadModel> getCategoriesByParentId(String parentId) {
        String cacheKey = CACHE_KEY_PARENT + parentId;

        List<CategoryReadModel> cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 检查熔断状态
        if (!shouldSkipRedis()) {
            try {
                Object redisRaw = redisTemplate.opsForValue().get(cacheKey);
                if (redisRaw instanceof List<?> rawList && !rawList.isEmpty() && rawList.getFirst() instanceof CategoryReadModel) {
                    @SuppressWarnings("unchecked")
                    List<CategoryReadModel> redisCached = (List<CategoryReadModel>) redisRaw;
                    localCache.put(cacheKey, redisCached);
                    recordRedisSuccess();
                    return redisCached;
                }
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("读取分类缓存", "parentId=" + parentId, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("读取分类缓存", "parentId=" + parentId, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("读取分类缓存", "parentId=" + parentId, e);
            }
        } else {
            log.warn("action=circuit_breaker_skip_redis, operation=读取分类缓存, key={}", cacheKey);
        }

        List<CategoryDO> categories = categoryQueryRepository.findByParentId(parentId);
        if (categories == null) {
            categories = List.of();
        }
        List<CategoryReadModel> models = categories.stream().map(this::toReadModel).toList();

        localCache.put(cacheKey, models);

        // 写入 Redis（仅在熔断未开启时）
        if (!shouldSkipRedis()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, models, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
                recordRedisSuccess();
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("设置分类缓存", "parentId=" + parentId, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("设置分类缓存", "parentId=" + parentId, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("设置分类缓存", "parentId=" + parentId, e);
            }
        }

        return models;
    }

    @Override
    public Optional<CategoryReadModel> getCategoryById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        String cacheKey = CACHE_KEY_ID + id;

        // 检查熔断状态
        if (!shouldSkipRedis()) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof CategoryReadModel readModel) {
                    recordRedisSuccess();
                    return Optional.of(readModel);
                }
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("读取分类缓存", "id=" + id, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("读取分类缓存", "id=" + id, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("读取分类缓存", "id=" + id, e);
            }
        } else {
            log.warn("action=circuit_breaker_skip_redis, operation=读取分类缓存, key={}", cacheKey);
        }

        List<CategoryDO> allCategories = categoryQueryRepository.findByIds(List.of(id));
        if (allCategories != null && !allCategories.isEmpty()) {
            CategoryReadModel model = toReadModel(allCategories.getFirst());

            // 写入 Redis（仅在熔断未开启时）
            if (!shouldSkipRedis()) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, model, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
                    recordRedisSuccess();
                } catch (RedisConnectionFailureException e) {
                    handleRedisConnectionFailure("设置分类缓存", "id=" + id, e);
                } catch (QueryTimeoutException e) {
                    handleRedisTimeout("设置分类缓存", "id=" + id, e);
                } catch (Exception e) {
                    handleUnexpectedRedisError("设置分类缓存", "id=" + id, e);
                }
            }

            return Optional.of(model);
        }

        return Optional.empty();
    }

    @Override
    public void evictAll() {
        localCache.invalidateAll();

        // 检查熔断状态
        if (!shouldSkipRedis()) {
            try {
                var keys = CacheUtils.scan(redisTemplate, CACHE_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                recordRedisSuccess();
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("清除分类缓存", "all", e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("清除分类缓存", "all", e);
            } catch (Exception e) {
                handleUnexpectedRedisError("清除分类缓存", "all", e);
            }
        } else {
            log.warn("action=circuit_breaker_skip_redis, operation=清除分类缓存, pattern={}", CACHE_KEY_PREFIX + "*");
        }
    }

    @Override
    public void evictByLevel(Integer level) {
        String cacheKey = CACHE_KEY_LEVEL + level;
        localCache.invalidate(cacheKey);

        // 检查熔断状态
        if (!shouldSkipRedis()) {
            try {
                redisTemplate.delete(cacheKey);
                recordRedisSuccess();
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("清除分类缓存", "level=" + level, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("清除分类缓存", "level=" + level, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("清除分类缓存", "level=" + level, e);
            }
        } else {
            log.warn("action=circuit_breaker_skip_redis, operation=清除分类缓存, key={}", cacheKey);
        }
    }

    @Override
    public void evictByParentId(String parentId) {
        String cacheKey = CACHE_KEY_PARENT + parentId;
        localCache.invalidate(cacheKey);

        // 检查熔断状态
        if (!shouldSkipRedis()) {
            try {
                redisTemplate.delete(cacheKey);
                recordRedisSuccess();
            } catch (RedisConnectionFailureException e) {
                handleRedisConnectionFailure("清除分类缓存", "parentId=" + parentId, e);
            } catch (QueryTimeoutException e) {
                handleRedisTimeout("清除分类缓存", "parentId=" + parentId, e);
            } catch (Exception e) {
                handleUnexpectedRedisError("清除分类缓存", "parentId=" + parentId, e);
            }
        } else {
            log.warn("action=circuit_breaker_skip_redis, operation=清除分类缓存, key={}", cacheKey);
        }
    }

    // ==================== 熔断状态管理 ====================

    /**
     * 判断是否应该跳过 Redis 操作（熔断开启或连续失败达到阈值）
     */
    private boolean shouldSkipRedis() {
        resetCircuitBreakerIfNeeded();
        return circuitBreakerOpen.get() || redisFailureCount.get() >= CIRCUIT_BREAKER_THRESHOLD;
    }

    /**
     * 记录 Redis 成功，重置失败计数器和熔断状态
     */
    private void recordRedisSuccess() {
        if (redisFailureCount.get() > 0 || circuitBreakerOpen.get()) {
            log.info("action=redis_recovered, previous_failure_count={}", redisFailureCount.get());
        }
        redisFailureCount.set(0);
        circuitBreakerOpen.set(false);
    }

    /**
     * 记录 Redis 失败，增加计数器，检查是否需要触发熔断
     */
    private void recordRedisFailure() {
        int count = redisFailureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        if (count >= CIRCUIT_BREAKER_THRESHOLD && !circuitBreakerOpen.get()) {
            circuitBreakerOpen.set(true);
            log.error("action=circuit_breaker_opened, failure_count={}, threshold={}, message=Redis连续失败达到阈值，触发熔断保护",
                    count, CIRCUIT_BREAKER_THRESHOLD);
        }
    }

    /**
     * 定期重置熔断状态（距离上次失败超过重置间隔）
     */
    private void resetCircuitBreakerIfNeeded() {
        long lastFailure = lastFailureTime.get();
        if (circuitBreakerOpen.get() && System.currentTimeMillis() - lastFailure > CIRCUIT_BREAKER_RESET_INTERVAL_MS) {
            circuitBreakerOpen.set(false);
            redisFailureCount.set(0);
            log.info("action=circuit_breaker_reset, message=熔断重置，允许重新尝试Redis操作");
        }
    }

    // ==================== 异常处理 ====================

    /**
     * 处理 Redis 连接失败（需要告警）
     */
    private void handleRedisConnectionFailure(String operation, String context, RedisConnectionFailureException e) {
        recordRedisFailure();
        log.error("action=redis_connection_failure, operation={}, context={}, failure_count={}, message=Redis连接失败，请检查Redis服务状态",
                operation, context, redisFailureCount.get(), e);
    }

    /**
     * 处理 Redis 超时（需要告警）
     */
    private void handleRedisTimeout(String operation, String context, QueryTimeoutException e) {
        recordRedisFailure();
        log.error("action=redis_timeout, operation={}, context={}, failure_count={}, message=Redis操作超时，请检查Redis性能",
                operation, context, redisFailureCount.get(), e);
    }

    /**
     * 处理其他 Redis 异常（需要关注）
     */
    private void handleUnexpectedRedisError(String operation, String context, Exception e) {
        recordRedisFailure();
        log.warn("action=redis_unexpected_error, operation={}, context={}, failure_count={}, message=Redis操作异常",
                operation, context, redisFailureCount.get(), e);
    }

    private CategoryReadModel toReadModel(CategoryDO category) {
        return new CategoryReadModel(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getIcon(),
                category.getSortOrder(),
                category.getStatus(),
                category.getCreateTime(),
                0
        );
    }
}
