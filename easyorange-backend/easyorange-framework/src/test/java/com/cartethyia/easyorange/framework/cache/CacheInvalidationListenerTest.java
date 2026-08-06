package com.cartethyia.easyorange.framework.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * L1 缓存跨节点失效广播 — 单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>{@link CacheInvalidationListener#publishInvalidation} 通过 convertAndSend 发布字符串消息</li>
 *   <li>{@link CacheInvalidationListener#handleMessage} 解析消息并失效对应 L1 缓存</li>
 *   <li>容错：异常消息体 / 未知 prefix / Redis 异常均不向上抛出（fail-open）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("L1 缓存跨节点失效广播")
class CacheInvalidationListenerTest {

    private static final String SEPARATOR = "\u0001";
    private static final String CHANNEL = CacheInvalidationListener.CHANNEL;

    @Mock
    private StringRedisTemplate redisTemplate;

    private CacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CacheInvalidationListener(redisTemplate);
    }

    @Test
    @DisplayName("publishInvalidation 通过 convertAndSend 发布到指定频道")
    void publishInvalidation_shouldConvertAndSendToChannel() {
        var prefix = "mlc:";
        var key = "product:1";

        listener.publishInvalidation(prefix, key);

        verify(redisTemplate).convertAndSend(CHANNEL, prefix + SEPARATOR + key);
    }

    @Test
    @DisplayName("publishInvalidation 当 Redis 异常时 fail-open，不向上抛出")
    void publishInvalidation_whenRedisThrows_shouldNotPropagate() {
        when(redisTemplate.convertAndSend(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertThatCode(() -> listener.publishInvalidation("mlc:", "k1"))
                .as("Redis 异常不应向上传播，避免拖垮调用方")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleMessage 收到合法消息时失效对应 prefix 的 L1 缓存")
    void handleMessage_validMessage_shouldInvalidateL1Cache() {
        Cache<String, Object> l1Cache = Caffeine.newBuilder().build();
        l1Cache.put("product:1", "stale-value");
        listener.register("mlc:", l1Cache);

        listener.handleMessage("mlc:" + SEPARATOR + "product:1");

        assertThat(l1Cache.getIfPresent("product:1")).as("L1 缓存应被失效").isNull();
    }

    @Test
    @DisplayName("handleMessage 仅失效对应 prefix 的 L1 缓存，不影响其他 prefix")
    void handleMessage_shouldOnlyInvalidateMatchingPrefix() {
        Cache<String, Object> l1CacheA = Caffeine.newBuilder().build();
        l1CacheA.put("k1", "v1");
        Cache<String, Object> l1CacheB = Caffeine.newBuilder().build();
        l1CacheB.put("k1", "v1");

        listener.register("mlc:", l1CacheA);
        listener.register("ai:pricing:", l1CacheB);

        listener.handleMessage("mlc:" + SEPARATOR + "k1");

        assertThat(l1CacheA.getIfPresent("k1")).isNull();
        assertThat(l1CacheB.getIfPresent("k1")).as("其他 prefix 的 L1 缓存不应被影响").isEqualTo("v1");
    }

    @Test
    @DisplayName("handleMessage 收到无分隔符的非法消息体时安全忽略")
    void handleMessage_malformedMessage_shouldNoop() {
        Cache<String, Object> l1Cache = Caffeine.newBuilder().build();
        l1Cache.put("k1", "v1");
        listener.register("mlc:", l1Cache);

        assertThatCode(() -> listener.handleMessage("no-separator-here")).doesNotThrowAnyException();

        assertThat(l1Cache.getIfPresent("k1")).as("非法消息不应触发 L1 失效").isEqualTo("v1");
    }

    @Test
    @DisplayName("handleMessage 收到未知 prefix 时安全忽略")
    void handleMessage_unknownPrefix_shouldNoop() {
        Cache<String, Object> l1Cache = Caffeine.newBuilder().build();
        l1Cache.put("k1", "v1");
        listener.register("mlc:", l1Cache);

        listener.handleMessage("unknown-prefix" + SEPARATOR + "k1");

        assertThat(l1Cache.getIfPresent("k1")).as("未知 prefix 的消息不应触发 L1 失效").isEqualTo("v1");
    }

    @Test
    @DisplayName("handleMessage 当 L1 invalidate 抛异常时 fail-open，不向上传播")
    void handleMessage_whenInvalidateThrows_shouldNotPropagate() {
        @SuppressWarnings("unchecked")
        Cache<String, Object> l1Cache = mock(Cache.class);
        doThrow(new RuntimeException("Caffeine OOM")).when(l1Cache).invalidate(any());

        listener.register("mlc:", l1Cache);

        assertThatCode(() -> listener.handleMessage("mlc:" + SEPARATOR + "k1"))
                .as("L1 失效异常不应向上传播，避免拖垮 Redis MessageListenerContainer")
                .doesNotThrowAnyException();

        verify(l1Cache, times(1)).invalidate("k1");
    }

    @Test
    @DisplayName("register 支持覆盖同名 prefix 的 L1 缓存实例")
    void register_samePrefixTwice_shouldOverwrite() {
        Cache<String, Object> l1CacheA = Caffeine.newBuilder().build();
        l1CacheA.put("k1", "vA");
        Cache<String, Object> l1CacheB = Caffeine.newBuilder().build();
        l1CacheB.put("k1", "vB");

        listener.register("mlc:", l1CacheA);
        listener.register("mlc:", l1CacheB);

        listener.handleMessage("mlc:" + SEPARATOR + "k1");

        assertThat(l1CacheA.getIfPresent("k1")).as("旧 L1 实例不应被影响").isEqualTo("vA");
        assertThat(l1CacheB.getIfPresent("k1")).as("新注册的 L1 实例应被失效").isNull();
    }
}
