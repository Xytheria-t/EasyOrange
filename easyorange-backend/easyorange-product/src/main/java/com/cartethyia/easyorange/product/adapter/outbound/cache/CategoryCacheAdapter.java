package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.cache.CacheUtils;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分类缓存适配器
 *
 * <p>缓存拓扑：L1 Caffeine → L2 Redis → DB 三级降级。
 *
 * <p>熔断保护：使用 Resilience4j CircuitBreaker 保护 Redis 操作。
 * 连续失败率超过阈值时自动开路，避免对已故障的 Redis 做无效重试，
 * 所有请求直接降级到 DB + 本地缓存。等待时间过后进入 Half-Open 探测恢复。
 *
 * <p>Micrometer 指标：熔断器状态切换、调用计数、耗时百分位自动上报到 Prometheus。
 */
@Slf4j
@Component
public class CategoryCacheAdapter implements CategoryCachePort {

    private final CategoryQueryRepository categoryQueryRepository;
    private final Cache<String, List<CategoryReadModel>> localCache;
    private final CircuitBreaker redisCircuitBreaker;
    private final RedisTemplate<Object, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "eo:category:list";
    private static final String CACHE_KEY_LEVEL = CACHE_KEY_PREFIX + ":level:";
    private static final String CACHE_KEY_PARENT = CACHE_KEY_PREFIX + ":parent:";
    private static final String CACHE_KEY_ID = CACHE_KEY_PREFIX + ":id:";
    private static final long REDIS_EXPIRE_MINUTES = 120;
    private static final int LOCAL_CACHE_MAX_SIZE = 100;
    private static final long LOCAL_CACHE_EXPIRE_MINUTES = 30;

    private static final String CIRCUIT_BREAKER_NAME = "redisCategoryCache";

    public CategoryCacheAdapter(CategoryQueryRepository categoryQueryRepository,
                                RedisTemplate<Object, Object> redisTemplate,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        this.categoryQueryRepository = categoryQueryRepository;
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(LOCAL_CACHE_MAX_SIZE)
                .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
        this.redisCircuitBreaker = circuitBreakerRegistry
                .circuitBreaker(CIRCUIT_BREAKER_NAME);

        // 注册熔断事件监听（结构化日志）
        this.redisCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.info(
                        "action=circuit_breaker_state_transition, name={}, from={}, to={}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.warn(
                        "action=circuit_breaker_error, name={}, elapsed={}ms, throwable={}",
                        event.getCircuitBreakerName(),
                        event.getElapsedDuration().toMillis(),
                        event.getThrowable().toString()))
                .onCallNotPermitted(event -> log.warn(
                        "action=circuit_breaker_call_blocked, name={}",
                        event.getCircuitBreakerName()));
    }

    @Override
    public List<CategoryReadModel> getCategoriesByLevel(Integer level) {
        String cacheKey = CACHE_KEY_LEVEL + level;

        List<CategoryReadModel> cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 熔断保护的 Redis 读取
        List<CategoryReadModel> redisCached = tryRedisGet(cacheKey, CategoryReadModel.class);
        if (redisCached != null) {
            localCache.put(cacheKey, redisCached);
            return redisCached;
        }

        // 降级：读 DB
        List<CategoryReadModel> models = categoryQueryRepository.findByLevel(level);
        if (models == null) {
            models = List.of();
        }
        localCache.put(cacheKey, models);

        // 异步写回 Redis（熔断未开启时）
        tryRedisSet(cacheKey, models);
        return models;
    }

    @Override
    public List<CategoryReadModel> getCategoriesByParentId(String parentId) {
        String cacheKey = CACHE_KEY_PARENT + parentId;

        List<CategoryReadModel> cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 熔断保护的 Redis 读取
        List<CategoryReadModel> redisCached = tryRedisGet(cacheKey, CategoryReadModel.class);
        if (redisCached != null) {
            localCache.put(cacheKey, redisCached);
            return redisCached;
        }

        // 降级：读 DB
        List<CategoryReadModel> models = categoryQueryRepository.findByParentId(parentId);
        if (models == null) {
            models = List.of();
        }
        localCache.put(cacheKey, models);

        // 异步写回 Redis（熔断未开启时）
        tryRedisSet(cacheKey, models);
        return models;
    }

    @Override
    public Optional<CategoryReadModel> getCategoryById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        String cacheKey = CACHE_KEY_ID + id;

        // 熔断保护的 Redis 读取
        Supplier<Optional<CategoryReadModel>> redisGet = decorateRedisGetOptional(cacheKey, CategoryReadModel.class);
        Optional<CategoryReadModel> redisCached = tryWithFallback(redisGet, "getCategoryById", id);
        if (redisCached != null && redisCached.isPresent()) {
            return redisCached;
        }

        // 降级：读 DB
        List<CategoryReadModel> allCategories = categoryQueryRepository.findByIds(List.of(id));
        if (allCategories != null && !allCategories.isEmpty()) {
            CategoryReadModel model = allCategories.getFirst();
            // 写入 Redis（熔断未开启时）
            tryRedisSet(cacheKey, model);
            return Optional.of(model);
        }

        return Optional.empty();
    }

    @Override
    public void evictAll() {
        localCache.invalidateAll();
        tryRedisEvict(CACHE_KEY_PREFIX + "*");
    }

    @Override
    public void evictByLevel(Integer level) {
        String cacheKey = CACHE_KEY_LEVEL + level;
        localCache.invalidate(cacheKey);
        tryRedisDelete(cacheKey);
    }

    @Override
    public void evictByParentId(String parentId) {
        String cacheKey = CACHE_KEY_PARENT + parentId;
        localCache.invalidate(cacheKey);
        tryRedisDelete(cacheKey);
    }

    // ==================== Resilience4j 辅助方法 ====================

    /**
     * 通过熔断器执行 Redis GET 并尝试类型安全的 cast。
     * 熔断开路或 Redis 异常时返回 null，由调用方降级到 DB。
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> tryRedisGet(String key, Class<T> elementType) {
        Supplier<List<T>> redisGet = decorateRedisGetList(key, elementType);
        return tryWithFallback(redisGet, "redis_get", key);
    }

    /**
     * 通过熔断器执行 Redis SET（写入操作，失败可容忍）。
     */
    private void tryRedisSet(String key, Object value) {
        Runnable redisSet = decorateRedisSet(key, value);
        tryWithFallback(redisSet, "redis_set", key);
    }

    /**
     * 通过熔断器执行 Redis DELETE（单个 key）。
     */
    private void tryRedisDelete(String key) {
        Runnable redisDelete = decorateRedisDelete(key);
        tryWithFallback(redisDelete, "redis_delete", key);
    }

    /**
     * 通过熔断器执行 Redis EVICT（scan + delete 批量）。
     */
    private void tryRedisEvict(String pattern) {
        Runnable redisEvict = decorateRedisEvict(pattern);
        tryWithFallback(redisEvict, "redis_evict", pattern);
    }

    // ==================== Resilience4j 装饰器 ====================

    /**
     * 为 List 类型的 Redis GET 创建熔断保护装饰器。
     */
    @SuppressWarnings("unchecked")
    private <T> Supplier<List<T>> decorateRedisGetList(String key, Class<T> elementType) {
        return CircuitBreaker.decorateSupplier(redisCircuitBreaker, () -> {
            Object raw = redisTemplate.opsForValue().get(key);
            // 类型安全的 List 检查
            if (raw instanceof List<?> rawList && !rawList.isEmpty() && elementType.isInstance(rawList.getFirst())) {
                return (List<T>) rawList;
            }
            return null;
        });
    }

    /**
     * 为单个对象的 Redis GET 创建熔断保护装饰器。
     */
    private <T> Supplier<T> decorateRedisGet(String key, Class<T> type) {
        return CircuitBreaker.decorateSupplier(redisCircuitBreaker, () -> {
            Object raw = redisTemplate.opsForValue().get(key);
            if (type.isInstance(raw)) {
                return type.cast(raw);
            }
            return null;
        });
    }

    /**
     * 为 Redis GET-List 且返回 Optional 创建熔断保护装饰器。
     */
    @SuppressWarnings("unchecked")
    private <T> Supplier<Optional<T>> decorateRedisGetOptional(String key, Class<T> type) {
        return CircuitBreaker.decorateSupplier(redisCircuitBreaker, () -> {
            Object raw = redisTemplate.opsForValue().get(key);
            if (type.isInstance(raw)) {
                return Optional.of(type.cast(raw));
            }
            return Optional.empty();
        });
    }

    /**
     * 为 Redis SET 创建熔断保护装饰器。
     */
    private Runnable decorateRedisSet(String key, Object value) {
        return CircuitBreaker.decorateRunnable(redisCircuitBreaker, () ->
                redisTemplate.opsForValue().set(key, value, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * 为 Redis DELETE 创建熔断保护装饰器。
     */
    private Runnable decorateRedisDelete(String key) {
        return CircuitBreaker.decorateRunnable(redisCircuitBreaker, () ->
                redisTemplate.delete(key));
    }

    /**
     * 为 Redis 批量 evict（scan + delete）创建熔断保护装饰器。
     */
    private Runnable decorateRedisEvict(String pattern) {
        return CircuitBreaker.decorateRunnable(redisCircuitBreaker, () -> {
            var keys = CacheUtils.scan(redisTemplate, pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        });
    }

    // ==================== 执行 + 降级 ====================

    /**
     * 执行熔断保护的 Supplier，异常时降级返回 null。
     */
    private <T> T tryWithFallback(Supplier<T> supplier, String operation, String context) {
        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("action=circuit_breaker_open, operation={}, context={}, message=熔断开路，跳过Redis",
                    operation, context);
            return null;
        } catch (RedisConnectionFailureException e) {
            log.error("action=redis_connection_failure, operation={}, context={}, message=Redis连接失败",
                    operation, context, e);
            return null;
        } catch (QueryTimeoutException e) {
            log.error("action=redis_timeout, operation={}, context={}, message=Redis操作超时",
                    operation, context, e);
            return null;
        } catch (Exception e) {
            log.warn("action=redis_unexpected_error, operation={}, context={}, message=Redis操作异常",
                    operation, context, e);
            return null;
        }
    }

    /**
     * 执行熔断保护的 Runnable，异常时静默降级。
     */
    private void tryWithFallback(Runnable runnable, String operation, String context) {
        try {
            runnable.run();
        } catch (CallNotPermittedException e) {
            log.warn("action=circuit_breaker_open, operation={}, context={}, message=熔断开路，跳过Redis",
                    operation, context);
        } catch (RedisConnectionFailureException e) {
            log.error("action=redis_connection_failure, operation={}, context={}, message=Redis连接失败",
                    operation, context, e);
        } catch (QueryTimeoutException e) {
            log.error("action=redis_timeout, operation={}, context={}, message=Redis操作超时",
                    operation, context, e);
        } catch (Exception e) {
            log.warn("action=redis_unexpected_error, operation={}, context={}, message=Redis操作异常",
                    operation, context, e);
        }
    }
}
