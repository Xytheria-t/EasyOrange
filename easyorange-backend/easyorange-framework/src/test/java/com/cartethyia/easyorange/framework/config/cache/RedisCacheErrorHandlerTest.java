package com.cartethyia.easyorange.framework.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

/**
 * CacheErrorHandler fail-open 打点 — 吞异常可以、吞统计不行。
 * <p>
 * Redis 故障时读降级直查 DB、写放弃缓存都「功能正确」，此前只有日志：降级频率靠 grep、
 * 缓存故障率不可出数。四路故障必须各自进 {@code easyorange.cache.failures}，按 op + cache 打标。
 */
@DisplayName("RedisCacheConfig CacheErrorHandler fail-open 计数")
class RedisCacheErrorHandlerTest {

    private static final String CACHE_NAME = "eo:category:list";

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final RedisCacheConfig config =
            new RedisCacheConfig(new CacheProperties(null, Duration.ofMinutes(30), 0.1), meterRegistry);

    @Test
    @DisplayName("get/put/evict/clear 四路故障各自计数（op + cache 打标）")
    void errorHandler_countsFailuresPerOp() {
        var errorHandler = config.errorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn(CACHE_NAME);
        var ex = new RuntimeException("connection refused");

        errorHandler.handleCacheGetError(ex, cache, "k1");
        errorHandler.handleCachePutError(ex, cache, "k1", "v");
        errorHandler.handleCacheEvictError(ex, cache, "k1");
        errorHandler.handleCacheClearError(ex, cache);

        for (String op : new String[] {"get", "put", "evict", "clear"}) {
            var counter = meterRegistry
                    .find("easyorange.cache.failures")
                    .tags("op", op, "cache", CACHE_NAME)
                    .counter();
            assertThat(counter).as("op=%s 应有计数", op).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
}
