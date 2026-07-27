package com.cartethyia.easyorange.framework.event.dlq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExponentialBackoffRetryStrategy 指数退避重试策略")
class ExponentialBackoffRetryStrategyTest {

    private final ExponentialBackoffRetryStrategy strategy = new ExponentialBackoffRetryStrategy();

    @Nested
    @DisplayName("shouldRetry")
    class ShouldRetryTests {

        @Test
        @DisplayName("重试次数 0 应允许重试")
        void shouldRetry_zeroRetry_returnsTrue() {
            assertThat(strategy.shouldRetry(0)).isTrue();
        }

        @Test
        @DisplayName("重试次数 2 应允许重试（未达上限 3）")
        void shouldRetry_belowMax_returnsTrue() {
            assertThat(strategy.shouldRetry(2)).isTrue();
        }

        @Test
        @DisplayName("重试次数 3 不应重试（已达上限）")
        void shouldRetry_atMax_returnsFalse() {
            assertThat(strategy.shouldRetry(3)).isFalse();
        }

        @Test
        @DisplayName("重试次数超过上限不应重试")
        void shouldRetry_aboveMax_returnsFalse() {
            assertThat(strategy.shouldRetry(99)).isFalse();
        }
    }

    @Nested
    @DisplayName("getDelayMillis")
    class GetDelayMillisTests {

        @Test
        @DisplayName("第 1 次重试延迟 1 分钟")
        void getDelayMillis_firstRetry_60seconds() {
            assertThat(strategy.getDelayMillis(0)).isEqualTo(60_000L);
        }

        @Test
        @DisplayName("第 2 次重试延迟 5 分钟")
        void getDelayMillis_secondRetry_5minutes() {
            assertThat(strategy.getDelayMillis(1)).isEqualTo(300_000L);
        }

        @Test
        @DisplayName("第 3 次重试延迟 15 分钟")
        void getDelayMillis_thirdRetry_15minutes() {
            assertThat(strategy.getDelayMillis(2)).isEqualTo(900_000L);
        }

        @Test
        @DisplayName("超过数组范围返回最大延迟值")
        void getDelayMillis_outOfBounds_returnsMaxDelay() {
            assertThat(strategy.getDelayMillis(99)).isEqualTo(900_000L);
        }

        @Test
        @DisplayName("负值返回最小延迟值")
        void getDelayMillis_negative_returnsMinDelay() {
            assertThat(strategy.getDelayMillis(-1)).isEqualTo(60_000L);
        }
    }

    @Test
    @DisplayName("最大重试次数为 3")
    void getMaxRetries_returns3() {
        assertThat(strategy.getMaxRetries()).isEqualTo(3);
    }
}
