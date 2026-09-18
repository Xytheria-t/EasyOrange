package com.cartethyia.easyorange.framework.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashSet;
import java.util.function.DoubleSupplier;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * 抖动是随机行为，但断言不依赖随机结果：随机源由构造器注入，用例要么注入确定性取值钉住边界，
 * 要么注入固定种子 + 断言「对任意取值都成立」的不变式。生产走 ThreadLocalRandom。
 */
@DisplayName("TTL 抖动 RedisCacheWriter 测试（防雪崩）")
class JitterTtlRedisCacheWriterTest {

    private static final String NAME = "cache";
    private static final byte[] KEY = "k".getBytes();
    private static final byte[] VALUE = "v".getBytes();
    private static final Duration BASE = Duration.ofSeconds(60);
    /** ratio 0.5 时抖动的上界（取不到，实际最大 base + 29.999s）。 */
    private static final Duration MAX_JITTER = Duration.ofSeconds(30);

    private static JitterTtlRedisCacheWriter writer(RedisCacheWriter delegate, DoubleSupplier uniform) {
        return new JitterTtlRedisCacheWriter(delegate, 0.5, uniform);
    }

    private static JitterTtlRedisCacheWriter writer(RedisCacheWriter delegate) {
        return new JitterTtlRedisCacheWriter(delegate, 0.5);
    }

    @Test
    @DisplayName("put：TTL 落在 [base, base+ratio·ttl) 内，多次写入错峰")
    void put_ttlJittered() {
        var delegate = mock(RedisCacheWriter.class);
        // 固定种子：分布形状可复现，且下界 inclusive / 上界 exclusive 对任意取值都成立，不会概率性挂
        var seeded = RandomGeneratorFactory.getDefault().create(42);
        var writer = writer(delegate, seeded::nextDouble);

        for (int i = 0; i < 50; i++) {
            writer.put(NAME, KEY, VALUE, BASE);
        }

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate, times(50)).put(eq(NAME), any(), any(), ttlCaptor.capture());
        var seen = new HashSet<Long>();
        for (Duration ttl : ttlCaptor.getAllValues()) {
            assertThat(ttl).isBetween(BASE, BASE.plus(MAX_JITTER));
            seen.add(ttl.toMillis());
        }
        assertThat(seen).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("store / putIfAbsent：同样加抖动，下界与中点可确定断言")
    void storeAndPutIfAbsent_jittered() {
        var delegate = mock(RedisCacheWriter.class);

        writer(delegate, () -> 0.0).store(NAME, KEY, VALUE, BASE); // 取到下界：抖动为 0，TTL 不退化为「必须大于 base」
        writer(delegate, () -> 0.5).putIfAbsent(NAME, KEY, VALUE, BASE); // 60s × 0.5 × 0.5 = +15s

        var storeCaptor = ArgumentCaptor.forClass(Duration.class);
        var putIfAbsentCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate).store(eq(NAME), any(), any(), storeCaptor.capture());
        verify(delegate).putIfAbsent(eq(NAME), any(), any(), putIfAbsentCaptor.capture());
        assertThat(storeCaptor.getValue()).isEqualTo(BASE);
        assertThat(putIfAbsentCaptor.getValue()).isEqualTo(BASE.plusSeconds(15));
    }

    @Test
    @DisplayName("抖动取不到上界：uniform 逼近 1 仍严格小于 base+ratio·ttl")
    void put_upperBoundExclusive() {
        var delegate = mock(RedisCacheWriter.class);
        var writer = writer(delegate, () -> Math.nextDown(1.0));

        writer.put(NAME, KEY, VALUE, BASE);

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate).put(eq(NAME), any(), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isGreaterThan(BASE).isLessThan(BASE.plus(MAX_JITTER));
    }

    @Test
    @DisplayName("统计收集器包装后抖动语义（含注入的随机源）不丢")
    void withStatisticsCollector_keepsJitter() {
        var delegate = mock(RedisCacheWriter.class);
        when(delegate.withStatisticsCollector(any())).thenReturn(delegate);

        var wrapped = writer(delegate, () -> 0.5).withStatisticsCollector(mock(CacheStatisticsCollector.class));
        wrapped.put(NAME, KEY, VALUE, BASE);

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate).put(eq(NAME), any(), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(BASE.plusSeconds(15));
    }

    @Test
    @DisplayName("null / 零 / 负 TTL：原样透传不抖动")
    void put_nullOrNonPositiveTtl_passthrough() {
        var delegate = mock(RedisCacheWriter.class);
        var writer = writer(delegate);

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

        writer.put(NAME, KEY, VALUE, BASE);

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(delegate).put(eq(NAME), any(), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(BASE);
    }
}
