package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachingVisionAdapter 测试")
class CachingVisionAdapterTest {

    @Mock
    private QwenVlVisionAdapter delegate;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private AiProperties.Cache cacheProps;

    @Mock
    private RedisCache redisCache;

    private Map<AiCallScope, MultiLevelCache> aiCaches;
    private Cache<String, Object> staleCache;
    private CachingVisionAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(aiProperties.getCache()).thenReturn(cacheProps);
        lenient().when(cacheProps.isEnabled()).thenReturn(true);

        staleCache = Caffeine.newBuilder().build();
        aiCaches = new EnumMap<>(AiCallScope.class);

        for (var scope : AiCallScope.values()) {
            Cache<String, Object> l1 = Caffeine.newBuilder().build();
            var mlc = new MultiLevelCache(l1, redisCache, scope.cacheKeyPrefix(), scope.getTtlSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            aiCaches.put(scope, mlc);
        }

        adapter = new CachingVisionAdapter(delegate, aiProperties, aiCaches, staleCache);
    }

    @Test
    @DisplayName("缓存禁用时直接调用 delegate")
    void cacheDisabled() {
        when(cacheProps.isEnabled()).thenReturn(false);
        when(delegate.analyzeImages(any(), eq("prompt"))).thenReturn("result");

        String result = adapter.analyzeImages(List.of("url1"), "prompt");

        assertThat(result).isEqualTo("result");
        verify(delegate).analyzeImages(List.of("url1"), "prompt");
    }

    @Test
    @DisplayName("analyzeImage 单图调用")
    void analyzeImage_single() {
        when(delegate.analyzeImages(any(), eq("prompt"))).thenReturn("result");

        String result = adapter.analyzeImage("url1", "prompt");

        assertThat(result).isEqualTo("result");
        verify(delegate).analyzeImages(List.of("url1"), "prompt");
    }

    @Test
    @DisplayName("analyzeImages 多图调用")
    void analyzeImages_multiple() {
        when(delegate.analyzeImages(any(), eq("prompt"))).thenReturn("result");

        String result = adapter.analyzeImages(List.of("url1", "url2"), "prompt");

        assertThat(result).isEqualTo("result");
        verify(delegate).analyzeImages(List.of("url1", "url2"), "prompt");
    }
}
