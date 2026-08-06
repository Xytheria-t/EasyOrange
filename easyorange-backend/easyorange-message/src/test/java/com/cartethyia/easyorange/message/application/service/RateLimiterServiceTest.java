package com.cartethyia.easyorange.message.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService 单元测试")
class RateLimiterServiceTest {

    @Mock
    private DistributedRateLimiter distributedRateLimiter;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private static final String USER_ID = "1";

    @Nested
    @DisplayName("allowSendMessage")
    class AllowSendMessageTests {

        @Test
        @DisplayName("获得令牌时放行")
        void allowSendMessage_acquiredToken_returnsTrue() {
            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(true);

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("令牌耗尽时限流")
        void allowSendMessage_tokenExhausted_returnsFalse() {
            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(false);

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis 异常时 fail-open 放行")
        void allowSendMessage_redisException_failsOpen() {
            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenThrow(new RuntimeException("Redis 连接失败"));

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("限流 key")
    class RateLimitKeyTests {

        @Test
        @DisplayName("key 包含用户 ID")
        void allowSendMessage_keyContainsUserId() {
            when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                    .thenReturn(true);

            rateLimiterService.allowSendMessage(USER_ID);

            org.mockito.Mockito.verify(distributedRateLimiter).tryAcquire("eo:rate:message:" + USER_ID, 5, 1);
        }
    }
}
