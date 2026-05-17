package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.framework.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService 单元测试")
class RateLimiterServiceTest {

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("allowSendMessage")
    class AllowSendMessageTests {

        @Test
        @DisplayName("首次发送消息时返回 true")
        void allowSendMessage_firstCall_returnsTrue() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(null);

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isTrue();
            verify(redisCache).set(anyString(), eq(1), eq(1L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("在限制次数内发送消息返回 true")
        void allowSendMessage_underLimit_returnsTrue() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(3);

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isTrue();
            verify(redisCache).increment(anyString(), eq(1L));
        }

        @Test
        @DisplayName("超过限制次数后返回 false")
        void allowSendMessage_overLimit_returnsFalse() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(5);

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isFalse();
            verify(redisCache, never()).increment(anyString(), anyLong());
        }

        @Test
        @DisplayName("正好达到限制次数时返回 false")
        void allowSendMessage_atExactLimit_returnsFalse() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(5);

            boolean result = rateLimiterService.allowSendMessage(USER_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis 异常时使用本地计数器降级")
        void allowSendMessage_redisException_usesLocalFallback() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenThrow(new RuntimeException("Redis 连接失败"));

            // 前 5 次应返回 true（本地计数器 0-4）
            for (int i = 0; i < 5; i++) {
                assertThat(rateLimiterService.allowSendMessage(USER_ID)).isTrue();
            }
            // 第 6 次应返回 false
            assertThat(rateLimiterService.allowSendMessage(USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Redis 异常降级后本地计数器重置")
        void allowSendMessage_redisException_localCounterResets() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenThrow(new RuntimeException("Redis 连接失败"));

            // 触发限流
            for (int i = 0; i < 6; i++) {
                rateLimiterService.allowSendMessage(USER_ID);
            }

            // 本地计数器在达到 5 时置 0，所以后续调用应返回 true
            boolean result = rateLimiterService.allowSendMessage(USER_ID);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("allowTyping")
    class AllowTypingTests {

        @Test
        @DisplayName("首次发送 typing 时返回 true")
        void allowTyping_firstCall_returnsTrue() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(null);

            boolean result = rateLimiterService.allowTyping(USER_ID);

            assertThat(result).isTrue();
            verify(redisCache).set(anyString(), eq(1), eq(2L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("2 秒内再次发送 typing 返回 false")
        void allowTyping_within2Seconds_returnsFalse() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(1);

            boolean result = rateLimiterService.allowTyping(USER_ID);

            assertThat(result).isFalse();
            verify(redisCache, never()).increment(anyString(), anyLong());
        }

        @Test
        @DisplayName("Redis 异常时 typing 优雅降级返回 true")
        void allowTyping_redisException_returnsTrue() {
            when(redisCache.get(anyString(), eq(Integer.class))).thenThrow(new RuntimeException("Redis 连接失败"));

            boolean result = rateLimiterService.allowTyping(USER_ID);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Redis key 格式")
    class RedisKeyFormatTests {

        @Test
        @DisplayName("消息限流 key 包含用户 ID")
        void allowSendMessage_keyContainsUserId() {
            when(redisCache.get(eq("chat:rate:message:" + USER_ID), eq(Integer.class))).thenReturn(null);

            rateLimiterService.allowSendMessage(USER_ID);

            verify(redisCache).set(eq("chat:rate:message:" + USER_ID), eq(1), eq(1L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("typing 限流 key 包含用户 ID")
        void allowTyping_keyContainsUserId() {
            when(redisCache.get(eq("chat:rate:typing:" + USER_ID), eq(Integer.class))).thenReturn(null);

            rateLimiterService.allowTyping(USER_ID);

            verify(redisCache).set(eq("chat:rate:typing:" + USER_ID), eq(1), eq(2L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("消息限流 key 正确格式化")
        void allowSendMessage_correctKeyFormat() {
            Long userId = 12345L;
            when(redisCache.get(anyString(), eq(Integer.class))).thenReturn(null);

            rateLimiterService.allowSendMessage(userId);

            verify(redisCache).get("chat:rate:message:12345", Integer.class);
        }
    }
}
