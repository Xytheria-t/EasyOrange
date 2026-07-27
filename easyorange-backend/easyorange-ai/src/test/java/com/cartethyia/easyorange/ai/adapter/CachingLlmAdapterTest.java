package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.retry.Retry;
import org.springframework.data.redis.core.RedisTemplate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachingLlmAdapter 测试")
class CachingLlmAdapterTest {

    @Mock
    private DeepSeekLlmAdapter delegate;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private AiProperties.Cache cacheProps;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOps;

    @Mock
    private AiMetricsService aiMetricsService;

    private Map<AiCallScope, MultiLevelCache> aiCaches;
    private Cache<String, Object> staleCache;
    private CachingLlmAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(aiProperties.getCache()).thenReturn(cacheProps);
        lenient().when(cacheProps.isEnabled()).thenReturn(true);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().doNothing().when(aiMetricsService).recordCacheHit(anyString());
        lenient().doNothing().when(aiMetricsService).recordCacheMiss(anyString());

        staleCache = Caffeine.newBuilder().build();
        aiCaches = new EnumMap<>(AiCallScope.class);

        for (var scope : AiCallScope.values()) {
            Cache<String, Object> l1 = Caffeine.newBuilder().build();
            var mlc = new MultiLevelCache(l1, redisTemplate, scope.cacheKeyPrefix(), scope.getTtlSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            aiCaches.put(scope, mlc);
        }

        adapter = new CachingLlmAdapter(delegate, aiProperties, aiCaches, staleCache, aiMetricsService,
                Retry.ofDefaults("test"), Bulkhead.ofDefaults("test"));
    }

    @Test
    @DisplayName("缓存禁用时直接调用 delegate")
    void cacheDisabled() {
        when(cacheProps.isEnabled()).thenReturn(false);
        when(delegate.generateText("sys", "user")).thenReturn("result");

        String result = adapter.generateText("sys", "user");

        assertThat(result).isEqualTo("result");
        verify(delegate).generateText("sys", "user");
    }

    @Test
    @DisplayName("generateEmbedding 不缓存")
    void embeddingNotCached() {
        when(delegate.generateEmbedding("text")).thenReturn(List.of(0.1f, 0.2f));

        var result = adapter.generateEmbedding("text");

        assertThat(result).containsExactly(0.1f, 0.2f);
        verify(delegate).generateEmbedding("text");
        verifyNoMoreInteractions(delegate);
    }

    @Test
    @DisplayName("generateText 正常调用")
    void generateText_normal() {
        when(delegate.generateText("sys", "user")).thenReturn("result");

        String result = adapter.generateText("sys", "user");

        assertThat(result).isEqualTo("result");
        verify(delegate).generateText("sys", "user");
    }

    @Test
    @DisplayName("generateTextWithJson 正常调用")
    void generateTextWithJson_normal() {
        when(delegate.generateTextWithJson("sys", "user")).thenReturn("{\"key\":\"value\"}");

        String result = adapter.generateTextWithJson("sys", "user");

        assertThat(result).isEqualTo("{\"key\":\"value\"}");
        verify(delegate).generateTextWithJson("sys", "user");
    }

    @Test
    @DisplayName("缓存命中时记录 cache hit 指标")
    void cacheHitRecordsMetric() {
        when(delegate.generateText("sys", "user")).thenReturn("result");
        // First call: miss → caches result
        adapter.generateText("sys", "user");
        // Second call: hit
        adapter.generateText("sys", "user");

        verify(aiMetricsService).recordCacheHit("REVIEW");
        verify(aiMetricsService).recordCacheMiss("REVIEW");
    }
}
