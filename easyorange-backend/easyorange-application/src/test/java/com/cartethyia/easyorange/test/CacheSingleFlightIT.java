package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 防击穿单飞守卫 —— {@code @Cacheable(sync = true)} 的「同 key 并发未命中只跑一次 loader」
 * 由 <b>cache writer</b> 承担，不是注解自己做的。两处都能让它静默失效（不报错、不降级）：
 * <ol>
 *   <li>writer 装配成 {@code nonLockingRedisCacheWriter} —— 2026-10-07 之前的实际状态；</li>
 *   <li>{@code JitterTtlRedisCacheWriter} 不透传带 loader 的那次 {@code get} —— 接口里它是
 *       {@code default}，默认实现读一次、未命中就跑 loader，把 writer 的加锁实现整个挡在外面。</li>
 * </ol>
 * 两条都是「功能看着在、实际没生效」，且此前那条同名单测跑的是 {@code ConcurrentMapCacheManager}
 * （其 {@code computeIfAbsent} 自带单飞）所以一直是绿的。这里用真实 Redis + 真实
 * {@link CacheManager} 从端到端钉住，任何一处回退都会红。
 */
@DisplayName("缓存防击穿单飞（真实 Redis）")
class CacheSingleFlightIT extends AbstractIntegrationTest {

    private static final int THREADS = 8;
    private static final String CACHE_NAME = "eo:it:single-flight";

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("并发未命中同 key -> loader 只跑一次，且各线程拿到同一个值")
    void concurrentMissRunsLoaderOnce() throws Exception {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).as("RedisCacheManager 应按需创建缓存").isNotNull();
        cache.clear();

        var loaderCalls = new AtomicInteger();
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return cache.get("k", () -> {
                            loaderCalls.incrementAndGet();
                            sleepQuietly(300); // 撑开并发窗口，让其余线程一定落在「加载中」
                            return "v";
                        });
                    } finally {
                        done.countDown();
                    }
                }));
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        for (Future<String> f : futures) {
            assertThat(f.get()).isEqualTo("v");
        }
        assertThat(loaderCalls).as("同 key 并发未命中只应跑一次 loader").hasValue(1);

        cache.clear();
    }

    @Test
    @DisplayName("第二次读走缓存 -> loader 不再被调用")
    void secondReadHitsCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        cache.clear();

        var loaderCalls = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            cache.get("k2", () -> {
                loaderCalls.incrementAndGet();
                return "v";
            });
        }

        assertThat(loaderCalls).hasValue(1);
        cache.clear();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
