package com.cartethyia.easyorange.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 多级缓存单元测试 — 覆盖跨节点 L1 失效广播、负缓存、单飞回源与 TTL 校验。
 * <ul>
 *   <li>构造时注册 L1 到 {@link CacheInvalidationListener}</li>
 *   <li>{@code evict} / {@code put} 触发失效广播</li>
 *   <li>{@code get} 回源填充<b>不</b>触发失效广播（避免无意义广播）</li>
 *   <li>回源为 null 时写入负缓存（哨兵），后续请求不重复回源</li>
 *   <li>并发 L1 未命中时仅一个线程回源（Caffeine 原子单飞）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MultiLevelCache 多级缓存测试")
class MultiLevelCacheTest {

    private static final String PREFIX = "mlc:";
    private static final String KEY = "product:1";
    private static final String L2_KEY = PREFIX + KEY;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOps;

    @Mock
    private CacheInvalidationListener listener;

    private Cache<String, Object> l1Cache;

    @BeforeEach
    void setUp() {
        l1Cache = Caffeine.newBuilder().build();
    }

    @Test
    @DisplayName("构造时向 listener 注册 L1 缓存（以 keyPrefix 为标识）")
    void constructor_withListener_shouldRegisterL1Cache() {
        new MultiLevelCache(l1Cache, redisTemplate, defaultConfig(), listener, null, null);

        verify(listener).register(PREFIX, l1Cache);
    }

    @Test
    @DisplayName("evict 同步失效 L1+L2，并发布失效广播")
    void evict_shouldInvalidateL1L2AndPublish() {
        l1Cache.put(KEY, "stale");
        when(redisTemplate.delete(L2_KEY)).thenReturn(true);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig(), listener, null, null);
        mlc.evict(KEY);

        assertThat(l1Cache.getIfPresent(KEY)).isNull();
        verify(redisTemplate).delete(L2_KEY);
        verify(listener).publishInvalidation(PREFIX, KEY);
    }

    @Test
    @DisplayName("put 写入 L1+L2，并发布失效广播（其他节点 L1 旧值需失效）")
    void put_shouldWriteL1L2AndPublish() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig(), listener, null, null);
        mlc.put(KEY, "new-value");

        assertThat(l1Cache.getIfPresent(KEY)).isEqualTo("new-value");
        verify(valueOps).set(eq(L2_KEY), eq("new-value"), eq(1800000L), eq(TimeUnit.MILLISECONDS));
        verify(listener).publishInvalidation(PREFIX, KEY);
    }

    @Test
    @DisplayName("put 当 key/value 为 null 时 no-op，不发布广播")
    void put_nullArgs_shouldNoop() {
        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig(), listener, null, null);

        mlc.put(null, "value");
        mlc.put(KEY, null);

        verify(redisTemplate, never()).opsForValue();
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("get 命中 L1 时不访问 L2，不发布广播")
    void get_l1Hit_shouldNotTouchL2OrPublish() {
        l1Cache.put(KEY, "cached");

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig(), listener, null, null);
        var result = mlc.get(KEY, String.class, () -> "should-not-be-called");

        assertThat(result).isEqualTo("cached");
        verify(redisTemplate, never()).opsForValue();
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("get 命中 L2 时回填 L1，不发布广播")
    void get_l2Hit_shouldBackfillL1WithoutPublish() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(L2_KEY)).thenReturn("from-redis");

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig(), listener, null, null);
        var result = mlc.get(KEY, String.class, () -> "should-not-be-called");

        assertThat(result).isEqualTo("from-redis");
        assertThat(l1Cache.getIfPresent(KEY)).isEqualTo("from-redis");
        verify(valueOps, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("get 回源填充 L1+L2，不发布失效广播")
    void get_miss_shouldBackfillL1L2WithoutPublish() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(L2_KEY)).thenReturn(null);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig());
        var result = mlc.get(KEY, String.class, () -> "fresh-value");

        assertThat(result).isEqualTo("fresh-value");
        assertThat(l1Cache.getIfPresent(KEY)).isEqualTo("fresh-value");
        verify(valueOps).set(eq(L2_KEY), eq("fresh-value"), eq(1800000L), eq(TimeUnit.MILLISECONDS));
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("回源为 null 时写入负缓存，L1 哨兵命中后不再重复回源")
    void get_loaderNull_shouldCacheNegativeAndSkipLoader() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(L2_KEY)).thenReturn(null);

        var config = new MultiLevelCache.Config(PREFIX, null,
                Duration.ofMinutes(30), Duration.ofSeconds(30), null, null);
        var mlc = new MultiLevelCache(l1Cache, redisTemplate, config);
        var calls = new AtomicInteger();

        String first = mlc.get(KEY, String.class, () -> {
            calls.incrementAndGet();
            return null;
        });
        assertThat(first).isNull();
        assertThat(calls.get()).isEqualTo(1);

        // L1 已缓存哨兵 → 二次 get 命中 L1，不再访问 L2 / 回源
        String second = mlc.get(KEY, String.class, () -> {
            calls.incrementAndGet();
            return "should-not-be-loaded";
        });
        assertThat(second).isNull();
        assertThat(calls.get()).isEqualTo(1);

        verify(valueOps).set(eq(L2_KEY), any(MultiLevelCache.NullValue.class), eq(30000L), eq(TimeUnit.MILLISECONDS));
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("并发 L1 未命中时仅一个线程回源（Caffeine 原子单飞）")
    void get_concurrentMiss_shouldLoadOnce() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(L2_KEY)).thenReturn(null);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig());
        var calls = new AtomicInteger();
        int threads = 8;
        var barrier = new CyclicBarrier(threads);
        var executor = Executors.newFixedThreadPool(threads);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    return mlc.get(KEY, String.class, () -> {
                        calls.incrementAndGet();
                        return "single";
                    });
                }));
            }
            for (var f : futures) {
                assertThat(f.get(5, TimeUnit.SECONDS)).isEqualTo("single");
            }
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("listener/redisson/metrics 均为 null 时所有方法正常工作，无 NPE")
    void noOptionalDeps_shouldWork() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.delete(L2_KEY)).thenReturn(true);
        when(valueOps.get(L2_KEY)).thenReturn(null);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, defaultConfig());

        var result = mlc.get(KEY, String.class, () -> "fresh");
        assertThat(result).isEqualTo("fresh");

        mlc.evict(KEY);
        verify(redisTemplate, times(1)).delete(L2_KEY);

        mlc.put(KEY, "updated");
        verify(valueOps).set(eq(L2_KEY), eq("updated"), eq(1800000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("l1Ttl 大于 l2Ttl 时构造校验失败")
    void config_l1TtlGreaterThanL2Ttl_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MultiLevelCache.Config(
                        PREFIX, Duration.ofMinutes(60), Duration.ofMinutes(30), null, null, null))
                .withMessageContaining("l1Ttl must be <= l2Ttl");
    }

    @Test
    @DisplayName("negativeTtl 大于 l2Ttl 时构造校验失败")
    void config_negativeTtlGreaterThanL2Ttl_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MultiLevelCache.Config(
                        PREFIX, null, Duration.ofMinutes(30), Duration.ofHours(1), null, null))
                .withMessageContaining("negativeTtl must be <= l2Ttl");
    }

    private static MultiLevelCache.Config defaultConfig() {
        return MultiLevelCache.Config.of(PREFIX, Duration.ofMinutes(30));
    }
}
