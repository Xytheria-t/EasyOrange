package com.cartethyia.easyorange.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 缓存跨节点失效广播 — 单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>{@link CacheInvalidationListener#publishInvalidation} 通过 RedisCallback 原始字节发布</li>
 *   <li>{@link CacheInvalidationListener#onMessage} 解析消息并失效对应 L1 缓存</li>
 *   <li>容错：异常消息体 / 未知 prefix / Redis 异常均不向上抛出（fail-open）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("L1 缓存跨节点失效广播")
class CacheInvalidationListenerTest {

    private static final String SEPARATOR = "\u0001";
    private static final String CHANNEL = CacheInvalidationListener.CHANNEL;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private RedisConnection connection;

    private CacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CacheInvalidationListener(redisTemplate);
    }

    @Test
    @DisplayName("publishInvalidation 通过 RedisCallback 原始字节发布到指定频道")
    void publishInvalidation_shouldPublishRawBytesToChannel() {
        var prefix = "mlc:";
        var key = "product:1";
        var expectedMessage = (prefix + SEPARATOR + key).getBytes(StandardCharsets.UTF_8);
        var expectedChannel = CHANNEL.getBytes(StandardCharsets.UTF_8);

        // 捕获 RedisCallback 并执行，验证 connection.publish 被正确调用
        when(redisTemplate.execute(any(RedisCallback.class), eq(true))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            callback.doInRedis(connection);
            return null;
        });

        listener.publishInvalidation(prefix, key);

        verify(connection).publish(expectedChannel, expectedMessage);
    }

    @Test
    @DisplayName("publishInvalidation 当 Redis 异常时 fail-open，不向上抛出")
    void publishInvalidation_whenRedisThrows_shouldNotPropagate() {
        when(redisTemplate.execute(any(RedisCallback.class), eq(true)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertThatCode(() -> listener.publishInvalidation("mlc:", "k1"))
                .as("Redis 异常不应向上传播，避免拖垮调用方")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onMessage 收到合法消息时失效对应 prefix 的 L1 缓存")
    void onMessage_validMessage_shouldInvalidateL1Cache() {
        Cache<String, Object> l1Cache = Caffeine.newBuilder().build();
        l1Cache.put("product:1", "stale-value");
        listener.register("mlc:", l1Cache);

        var body = ("mlc:" + SEPARATOR + "product:1").getBytes(StandardCharsets.UTF_8);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body);

        listener.onMessage(message, null);

        assertThat(l1Cache.getIfPresent("product:1"))
                .as("L1 缓存应被失效")
                .isNull();
    }

    @Test
    @DisplayName("onMessage 仅失效对应 prefix 的 L1 缓存，不影响其他 prefix")
    void onMessage_shouldOnlyInvalidateMatchingPrefix() {
        Cache<String, Object> l1CacheA = Caffeine.newBuilder().build();
        l1CacheA.put("k1", "v1");
        Cache<String, Object> l1CacheB = Caffeine.newBuilder().build();
        l1CacheB.put("k1", "v1");

        listener.register("mlc:", l1CacheA);
        listener.register("ai:pricing:", l1CacheB);

        var body = ("mlc:" + SEPARATOR + "k1").getBytes(StandardCharsets.UTF_8);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body);

        listener.onMessage(message, null);

        assertThat(l1CacheA.getIfPresent("k1")).isNull();
        assertThat(l1CacheB.getIfPresent("k1"))
                .as("其他 prefix 的 L1 缓存不应被影响")
                .isEqualTo("v1");
    }

    @Test
    @DisplayName("onMessage 收到无分隔符的非法消息体时安全忽略")
    void onMessage_malformedMessage_shouldNoop() {
        Cache<String, Object> l1Cache = Caffeine.newBuilder().build();
        l1Cache.put("k1", "v1");
        listener.register("mlc:", l1Cache);

        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("no-separator-here".getBytes(StandardCharsets.UTF_8));

        assertThatCode(() -> listener.onMessage(message, null))
                .doesNotThrowAnyException();

        assertThat(l1Cache.getIfPresent("k1"))
                .as("非法消息不应触发 L1 失效")
                .isEqualTo("v1");
    }

    @Test
    @DisplayName("onMessage 收到未知 prefix 时安全忽略")
    void onMessage_unknownPrefix_shouldNoop() {
        Cache<String, Object> l1Cache = Caffeine.newBuilder().build();
        l1Cache.put("k1", "v1");
        listener.register("mlc:", l1Cache);

        var body = ("unknown-prefix" + SEPARATOR + "k1").getBytes(StandardCharsets.UTF_8);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body);

        listener.onMessage(message, null);

        assertThat(l1Cache.getIfPresent("k1"))
                .as("未知 prefix 的消息不应触发 L1 失效")
                .isEqualTo("v1");
    }

    @Test
    @DisplayName("onMessage 当 L1 invalidate 抛异常时 fail-open，不向上传播")
    void onMessage_whenInvalidateThrows_shouldNotPropagate() {
        @SuppressWarnings("unchecked")
        Cache<String, Object> l1Cache = mock(Cache.class);
        doThrow(new RuntimeException("Caffeine OOM")).when(l1Cache).invalidate(any());

        listener.register("mlc:", l1Cache);

        var body = ("mlc:" + SEPARATOR + "k1").getBytes(StandardCharsets.UTF_8);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body);

        assertThatCode(() -> listener.onMessage(message, null))
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

        var body = ("mlc:" + SEPARATOR + "k1").getBytes(StandardCharsets.UTF_8);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body);

        listener.onMessage(message, null);

        assertThat(l1CacheA.getIfPresent("k1"))
                .as("旧 L1 实例不应被影响")
                .isEqualTo("vA");
        assertThat(l1CacheB.getIfPresent("k1"))
                .as("新注册的 L1 实例应被失效")
                .isNull();
    }
}
