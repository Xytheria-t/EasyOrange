package com.cartethyia.easyorange.framework.util;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分布式限流器单元测试 — 基于 Redisson RRateLimiter 令牌桶。
 * <p>
 * 验证：
 * <ul>
 *   <li>tryAcquire 调用 trySetRate（幂等配置）+ tryAcquire（获取令牌）</li>
 *   <li>trySetRate 使用 OVERALL 模式（全实例共享限流配额）</li>
 *   <li>返回值与 Redisson tryAcquire 一致</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedRateLimiter 令牌桶")
class DistributedRateLimiterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter rateLimiter;

    private DistributedRateLimiter distributedRateLimiter;

    @BeforeEach
    void setUp() {
        distributedRateLimiter = new DistributedRateLimiter(redissonClient);
    }

    @Test
    @DisplayName("tryAcquire 返回 true 时表示获得令牌（放行）")
    void tryAcquire_allowed_shouldReturnTrue() {
        String key = "eo:rate:ip:127.0.0.1";
        when(redissonClient.getRateLimiter(key)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), any(Duration.class)))
                .thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        boolean result = distributedRateLimiter.tryAcquire(key, 30, 60);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("tryAcquire 返回 false 时表示令牌耗尽（限流）")
    void tryAcquire_denied_shouldReturnFalse() {
        String key = "eo:rate:ip:127.0.0.1";
        when(redissonClient.getRateLimiter(key)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), any(Duration.class)))
                .thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(false);

        boolean result = distributedRateLimiter.tryAcquire(key, 30, 60);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("trySetRate 使用 OVERALL 模式（所有实例共享配额）+ 正确参数")
    void tryAcquire_shouldCallTrySetRateWithOverallMode() {
        String key = "eo:rate:ip:127.0.0.1";
        when(redissonClient.getRateLimiter(key)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), any(Duration.class)))
                .thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        distributedRateLimiter.tryAcquire(key, 30, 60);

        verify(rateLimiter).trySetRate(
                eq(RateType.OVERALL),
                eq(30L),
                eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("tryAcquire 每次只请求 1 个令牌")
    void tryAcquire_shouldRequestSinglePermit() {
        String key = "eo:rate:ip:127.0.0.1";
        when(redissonClient.getRateLimiter(key)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), any(Duration.class)))
                .thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        distributedRateLimiter.tryAcquire(key, 30, 60);

        // 仅调用 1 次 tryAcquire，且参数为 1（单个令牌）
        verify(rateLimiter, times(1)).tryAcquire(1);
    }

    @Test
    @DisplayName("trySetRate 幂等 — 已配置的限流器不会覆盖现有配置")
    void tryAcquire_whenRateAlreadySet_shouldNotOverwrite() {
        String key = "eo:rate:ip:127.0.0.1";
        when(redissonClient.getRateLimiter(key)).thenReturn(rateLimiter);
        // trySetRate 返回 false 表示配置已存在，不会覆盖
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), any(Duration.class)))
                .thenReturn(false);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        boolean result = distributedRateLimiter.tryAcquire(key, 30, 60);

        assertThat(result).isTrue();
        // 仍然调用 trySetRate（幂等检查），但不会覆盖已有配置
        verify(rateLimiter).trySetRate(
                eq(RateType.OVERALL), eq(30L), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("Redis 异常向上传播（由调用方决定 fail-open）")
    void tryAcquire_whenRedisThrows_shouldPropagate() {
        String key = "eo:rate:ip:127.0.0.1";
        when(redissonClient.getRateLimiter(key)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        org.assertj.core.api.Assertions.assertThatCode(
                        () -> distributedRateLimiter.tryAcquire(key, 30, 60))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Redis connection refused");
    }
}
