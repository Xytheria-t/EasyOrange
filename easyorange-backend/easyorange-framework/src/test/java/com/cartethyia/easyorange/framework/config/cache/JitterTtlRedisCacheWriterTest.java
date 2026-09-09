package com.cartethyia.easyorange.framework.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.cache.RedisCacheWriter;

@DisplayName("TTL 抖动 RedisCacheWriter 测试（防雪崩）")
class JitterTtlRedisCacheWriterTest {

    private static final String NAME = "cache";
    private static final byte[] KEY = "k".getBytes();
    private static final byte[] VALUE = "v".getBytes();

    @Test
    @DisplayName("put：TTL 加 0~ratio 随机偏移，多次写入错峰")
    void put_ttlJittered() {
        var delegate = mock(RedisCacheWriter.class);
        var writer = new JitterTtlRedisCacheWriter(delegate, 0.5);
        var base = Duration.ofSeconds(60);

        for (int i = 0; i < 50; i++) {
            writer.put(NAME, KEY, VALUE, base);
        }

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate, times(50)).put(eq(NAME), any(), any(), ttlCaptor.capture());
        var seen = new HashSet<Long>();
        for (Duration ttl : ttlCaptor.getAllValues()) {
            assertThat(ttl).isBetween(base, base.plusMillis(base.toMillis() / 2));
            seen.add(ttl.toMillis());
        }
        assertThat(seen.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("store / putIfAbsent：同样加抖动")
    void storeAndPutIfAbsent_jittered() {
        var delegate = mock(RedisCacheWriter.class);
        var writer = new JitterTtlRedisCacheWriter(delegate, 0.5);
        var base = Duration.ofSeconds(60);

        writer.store(NAME, KEY, VALUE, base);
        writer.putIfAbsent(NAME, KEY, VALUE, base);

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate).store(eq(NAME), any(), any(), ttlCaptor.capture());
        verify(delegate).putIfAbsent(eq(NAME), any(), any(), ttlCaptor.capture());
        for (Duration ttl : ttlCaptor.getAllValues()) {
            assertThat(ttl).isStrictlyBetween(base, base.plusSeconds(30));
        }
    }

    @Test
    @DisplayName("null / 零 / 负 TTL：原样透传不抖动")
    void put_nullOrNonPositiveTtl_passthrough() {
        var delegate = mock(RedisCacheWriter.class);
        var writer = new JitterTtlRedisCacheWriter(delegate, 0.5);

        writer.put(NAME, KEY, VALUE, null);
        writer.put(NAME, KEY, VALUE, Duration.ZERO);
        writer.put(NAME, KEY, VALUE, Duration.ofSeconds(-1));

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate, times(3)).put(eq(NAME), any(), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getAllValues()).containsExactly(null, Duration.ZERO, Duration.ofSeconds(-1));
    }

    @Test
    @DisplayName("比例 0（含负值收敛）：TTL 原样透传")
    void put_ratioClampedToZero_passthrough() {
        var delegate = mock(RedisCacheWriter.class);
        var writer = new JitterTtlRedisCacheWriter(delegate, -1);
        var base = Duration.ofSeconds(60);

        writer.put(NAME, KEY, VALUE, base);

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate).put(eq(NAME), any(), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(base);
    }
}
