package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginSecurityDomainService 测试")
class LoginSecurityDomainServiceTest {

    @Mock
    private RedisCache redisCache;

    private LoginSecurityDomainService loginSecurityDomainService;

    @BeforeEach
    void setUp() {
        loginSecurityDomainService = new LoginSecurityDomainService(redisCache);
    }

    @Nested
    @DisplayName("checkLoginAttempts")
    class CheckLoginAttemptsTests {

        @Test
        @DisplayName("无尝试记录时应通过")
        void shouldPassWhenNoAttempts() {
            // Arrange
            when(redisCache.get(any(), eq(Long.class))).thenReturn(null);

            // Act & Assert - 不应抛出异常
            loginSecurityDomainService.checkLoginAttempts("testuser");
        }

        @Test
        @DisplayName("尝试次数未达上限时应通过")
        void shouldPassWhenBelowMaxAttempts() {
            // Arrange
            when(redisCache.get(any(), eq(Long.class))).thenReturn(3L);

            // Act & Assert - 不应抛出异常
            loginSecurityDomainService.checkLoginAttempts("testuser");
        }

        @Test
        @DisplayName("尝试次数达到上限时应抛出异常")
        void shouldThrowWhenMaxAttemptsReached() {
            // Arrange
            when(redisCache.get(any(), eq(Long.class))).thenReturn((long) LoginCacheConstants.MAX_LOGIN_ATTEMPTS);

            // Act & Assert
            assertThatThrownBy(() -> loginSecurityDomainService.checkLoginAttempts("testuser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("尝试次数超过上限时应抛出异常")
        void shouldThrowWhenExceedsMaxAttempts() {
            // Arrange
            when(redisCache.get(any(), eq(Long.class))).thenReturn(10L);

            // Act & Assert
            assertThatThrownBy(() -> loginSecurityDomainService.checkLoginAttempts("testuser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("账号为空时应抛出异常")
        void shouldThrowWhenBlankAccount() {
            assertThatThrownBy(() -> loginSecurityDomainService.checkLoginAttempts(""))
                .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> loginSecurityDomainService.checkLoginAttempts(null))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("recordFailedAttempt")
    class RecordFailedAttemptTests {

        @Test
        @DisplayName("应递增计数器并设置过期时间")
        void shouldIncrementAndSetExpiry() {
            // Arrange
            when(redisCache.increment(any())).thenReturn(1L);

            // Act
            loginSecurityDomainService.recordFailedAttempt("testuser");

            // Assert
            verify(redisCache).increment(any());
            verify(redisCache).expire(any(), eq(LoginCacheConstants.ATTEMPTS_EXPIRE_TIME), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("达到最大尝试次数时应抛出异常")
        void shouldThrowWhenMaxReached() {
            // Arrange
            when(redisCache.increment(any())).thenReturn((long) LoginCacheConstants.MAX_LOGIN_ATTEMPTS);

            // Act & Assert
            assertThatThrownBy(() -> loginSecurityDomainService.recordFailedAttempt("testuser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("未达最大尝试次数时不应抛出异常")
        void shouldNotThrowWhenBelowMax() {
            // Arrange
            when(redisCache.increment(any())).thenReturn(2L);

            // Act & Assert - 不应抛出异常
            loginSecurityDomainService.recordFailedAttempt("testuser");
        }

        @Test
        @DisplayName("账号为空时应抛出异常")
        void shouldThrowWhenBlankAccount() {
            assertThatThrownBy(() -> loginSecurityDomainService.recordFailedAttempt(""))
                .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> loginSecurityDomainService.recordFailedAttempt(null))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("increment 返回 null 时不应抛出异常")
        void shouldHandleNullIncrementResult() {
            // Arrange
            when(redisCache.increment(any())).thenReturn(null);

            // Act & Assert - 不应抛出异常
            loginSecurityDomainService.recordFailedAttempt("testuser");
        }
    }

    @Nested
    @DisplayName("clearLoginAttempts")
    class ClearLoginAttemptsTests {

        @Test
        @DisplayName("应删除缓存 key")
        void shouldDeleteKey() {
            // Act
            loginSecurityDomainService.clearLoginAttempts("testuser");

            // Assert
            verify(redisCache).delete(any(String.class));
        }

        @Test
        @DisplayName("账号为空时应抛出异常")
        void shouldThrowWhenBlankAccount() {
            assertThatThrownBy(() -> loginSecurityDomainService.clearLoginAttempts(""))
                .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> loginSecurityDomainService.clearLoginAttempts(null))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("maskAccount")
    class MaskAccountTests {

        @Test
        @DisplayName("应正确脱敏邮箱")
        void shouldMaskEmail() {
            // Act
            String result = loginSecurityDomainService.maskAccount("test@example.com");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("****");
            assertThat(result).contains("@example.com");
        }

        @Test
        @DisplayName("应正确脱敏手机号")
        void shouldMaskPhone() {
            // Act
            String result = loginSecurityDomainService.maskAccount("13812345678");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("****");
            assertThat(result).startsWith("138");
            assertThat(result).endsWith("5678");
        }

        @Test
        @DisplayName("普通用户名应原样返回")
        void shouldReturnPlainUsername() {
            // Act
            String result = loginSecurityDomainService.maskAccount("testuser");

            // Assert
            assertThat(result).isEqualTo("testuser");
        }

        @Test
        @DisplayName("null 应返回 null")
        void shouldReturnNullForNull() {
            // Act
            String result = loginSecurityDomainService.maskAccount(null);

            // Assert
            assertThat(result).isNull();
        }
    }
}
