package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.cache.CacheInvalidationListener;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 分类缓存适配器 — 复用框架 {@link MultiLevelCache}（L1 Caffeine → L2 Redis → DB 回源，
 * 含单飞/负缓存/指标/Pub/Sub 跨节点失效），不再重复实现多级缓存。
 * <p>
 * Redis 不可用时 fail-open：{@link MultiLevelCache#get} 抛异常时降级直查 DB，
 * 与 {@link ProductCacheAdapter} 一致。
 */
@Slf4j
@Component
public class CategoryCacheAdapter implements CategoryCachePort {

    /** 分类缓存 L2 前缀（与旧版 CategoryCacheAdapter 的 key 格式保持一致，缓存可平滑复用） */
    private static final String PREFIX = "eo:category:list:";

    private static final Duration L1_TTL = Duration.ofMinutes(30);
    private static final Duration L2_TTL = Duration.ofMinutes(120);
    private static final Duration NEGATIVE_TTL = Duration.ofSeconds(30);
    private static final int LOCAL_CACHE_MAX_SIZE = 100;

    private final CategoryQueryRepository categoryQueryRepository;
    private final MultiLevelCache cache;

    /**
     * Spring 装配：在框架依赖之上构建独立的分类 {@link MultiLevelCache}。
     * 独立 L1（Caffeine）实例，避免与产品缓存共享，也避免额外 Spring bean 与
     * 框架 {@code multiLevelCache} 的 {@code @ConditionalOnMissingBean} 冲突。
     */
    @Autowired
    public CategoryCacheAdapter(
            CategoryQueryRepository categoryQueryRepository,
            RedisTemplate<Object, Object> redisTemplate,
            CacheInvalidationListener invalidationListener,
            ObjectProvider<RedissonClient> redissonClientProvider,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(
                categoryQueryRepository,
                buildCache(redisTemplate, invalidationListener, redissonClientProvider, meterRegistryProvider));
    }

    /** 测试缝：注入现成的 {@link MultiLevelCache}。 */
    CategoryCacheAdapter(CategoryQueryRepository categoryQueryRepository, MultiLevelCache cache) {
        this.categoryQueryRepository = categoryQueryRepository;
        this.cache = cache;
    }

    private static MultiLevelCache buildCache(
            RedisTemplate<Object, Object> redisTemplate,
            CacheInvalidationListener invalidationListener,
            ObjectProvider<RedissonClient> redissonClientProvider,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        Cache<String, Object> l1 = Caffeine.newBuilder()
                .maximumSize(LOCAL_CACHE_MAX_SIZE)
                .expireAfterWrite(L1_TTL.toMillis(), TimeUnit.MILLISECONDS)
                .build();
        var config = MultiLevelCache.Config.of(PREFIX, L1_TTL, L2_TTL, NEGATIVE_TTL);
        return new MultiLevelCache(
                l1,
                redisTemplate,
                config,
                invalidationListener,
                redissonClientProvider.getIfAvailable(),
                meterRegistryProvider.getIfAvailable());
    }

    @Override
    public List<CategoryReadModel> getCategoriesByLevel(Integer level) {
        return cachedList("level:" + level, () -> orEmpty(categoryQueryRepository.findByLevel(level)));
    }

    @Override
    public List<CategoryReadModel> getCategoriesByParentId(String parentId) {
        return cachedList("parent:" + parentId, () -> orEmpty(categoryQueryRepository.findByParentId(parentId)));
    }

    @Override
    public Optional<CategoryReadModel> getCategoryById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cachedSingle("id:" + id, () -> {
            var all = categoryQueryRepository.findByIds(List.of(id));
            return all != null && !all.isEmpty() ? all.getFirst() : null;
        }));
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    @Override
    public void evictByLevel(Integer level) {
        cache.evict("level:" + level);
    }

    @Override
    public void evictByParentId(String parentId) {
        cache.evict("parent:" + parentId);
    }

    // ── Private helpers ──

    @SuppressWarnings("unchecked")
    private List<CategoryReadModel> cachedList(String key, Supplier<List<CategoryReadModel>> loader) {
        try {
            return (List<CategoryReadModel>) cache.<List>get(key, List.class, loader::get);
        } catch (Exception e) {
            log.warn("action=category_cache_get_failed, key={}", key, e);
            return loader.get();
        }
    }

    private CategoryReadModel cachedSingle(String key, Supplier<CategoryReadModel> loader) {
        try {
            return cache.get(key, CategoryReadModel.class, loader);
        } catch (Exception e) {
            log.warn("action=category_cache_get_failed, key={}", key, e);
            return loader.get();
        }
    }

    private static List<CategoryReadModel> orEmpty(List<CategoryReadModel> list) {
        return list != null ? list : List.of();
    }
}
