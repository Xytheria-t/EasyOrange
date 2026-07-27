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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 多级缓存单元测试 — 重点关注跨节点 L1 失效广播集成。
 * <p>
 * 验证：
 * <ul>
 *   <li>构造时注册 L1 到 {@link CacheInvalidationListener}</li>
 *   <li>{@code evict} / {@code evictL2} / {@code put} 触发失效广播</li>
 *   <li>{@code get} 回源填充<b>不</b>触发失效广播（避免无意义广播）</li>
 *   <li>listener 为 null 时（旧构造器 / 测试场景）所有方法正常工作</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MultiLevelCache 跨节点 L1 失效广播集成")
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
    @DisplayName("构造时向 listener 注册 L1 缓存（以 l2KeyPrefix 为标识）")
    void constructor_withListener_shouldRegisterL1Cache() {
        new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);

        verify(listener).register(PREFIX, l1Cache);
    }

    @Test
    @DisplayName("evict 同步失效 L1+L2，并发布失效广播")
    void evict_shouldInvalidateL1L2AndPublish() {
        l1Cache.put(KEY, "stale");
        when(redisTemplate.delete(L2_KEY)).thenReturn(true);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        mlc.evict(KEY);

        assertThat(l1Cache.getIfPresent(KEY)).isNull();
        verify(redisTemplate).delete(L2_KEY);
        verify(listener).publishInvalidation(PREFIX, KEY);
    }

    @Test
    @DisplayName("evictL2 仅失效 L2，但仍然发布失效广播（其他节点 L1 副本必然陈旧）")
    void evictL2_shouldDeleteL2AndPublish() {
        when(redisTemplate.delete(L2_KEY)).thenReturn(true);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        mlc.evictL2(KEY);

        verify(redisTemplate).delete(L2_KEY);
        verify(listener).publishInvalidation(PREFIX, KEY);
    }

    @Test
    @DisplayName("put 写入 L1+L2，并发布失效广播（其他节点 L1 旧值需失效）")
    void put_shouldWriteL1L2AndPublish() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        mlc.put(KEY, "new-value");

        assertThat(l1Cache.getIfPresent(KEY)).isEqualTo("new-value");
        verify(valueOps).set(eq(L2_KEY), eq("new-value"), eq(30L), eq(TimeUnit.MINUTES));
        verify(listener).publishInvalidation(PREFIX, KEY);
    }

    @Test
    @DisplayName("put 当 key 为 null 时 no-op，不发布广播")
    void put_nullKey_shouldNoop() {
        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        mlc.put(null, "value");

        verify(redisTemplate, never()).opsForValue();
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("put 当 value 为 null 时 no-op，不发布广播")
    void put_nullValue_shouldNoop() {
        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        mlc.put(KEY, null);

        verify(redisTemplate, never()).opsForValue();
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("get 回源填充时不发布失效广播（新值填充，不涉及其他节点陈旧数据）")
    void get_cacheMiss_shouldBackfillWithoutPublish() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(L2_KEY)).thenReturn(null);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        var result = mlc.get(KEY, String.class, () -> "fresh-value");

        assertThat(result).isEqualTo("fresh-value");
        assertThat(l1Cache.getIfPresent(KEY)).isEqualTo("fresh-value");
        verify(valueOps).set(eq(L2_KEY), eq("fresh-value"), anyLong(), any(TimeUnit.class));
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("get 命中 L1 时不访问 L2，不发布广播")
    void get_l1Hit_shouldNotTouchL2OrPublish() {
        l1Cache.put(KEY, "cached");

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
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

        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES, listener);
        var result = mlc.get(KEY, String.class, () -> "should-not-be-called");

        assertThat(result).isEqualTo("from-redis");
        assertThat(l1Cache.getIfPresent(KEY)).isEqualTo("from-redis");
        verify(valueOps, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(listener, never()).publishInvalidation(anyString(), anyString());
    }

    @Test
    @DisplayName("listener 为 null 时（旧构造器）所有方法正常工作，无 NPE")
    void legacyConstructor_withoutListener_shouldWorkWithoutBroadcast() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.delete(L2_KEY)).thenReturn(true);
        when(valueOps.get(L2_KEY)).thenReturn(null);

        // 使用 5 参数旧构造器 — 测试场景或单节点部署
        var mlc = new MultiLevelCache(l1Cache, redisTemplate, PREFIX, 30, TimeUnit.MINUTES);

        // get 触发回源
        var result = mlc.get(KEY, String.class, () -> "fresh");
        assertThat(result).isEqualTo("fresh");

        // evict 不抛 NPE
        mlc.evict(KEY);
        verify(redisTemplate, times(1)).delete(L2_KEY);

        // put 不抛 NPE
        mlc.put(KEY, "updated");
        verify(valueOps).set(eq(L2_KEY), eq("updated"), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("2 参数旧构造器（默认 prefix 和 TTL）正常工作")
    void legacyConstructor_twoArgs_shouldWorkWithDefaults() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("mlc:" + KEY)).thenReturn(null);

        var mlc = new MultiLevelCache(l1Cache, redisTemplate);

        var result = mlc.get(KEY, String.class, () -> "default-prefix-value");
        assertThat(result).isEqualTo("default-prefix-value");
    }
}
