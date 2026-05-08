package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.user.domain.port.output.LoginAttemptPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginSecurityDomainService 测试")
class LoginSecurityDomainServiceTest {

    @Mock
    private LoginAttemptPort loginAttemptPort;

    private LoginSecurityDomainService loginSecurityDomainService;

    @BeforeEach
    void setUp() {
        loginSecurityDomainService = new LoginSecurityDomainService(loginAttemptPort);
    }

    @Nested
    @DisplayName("checkLoginAttempts")
    class CheckLoginAttemptsTests {

        @Test
        @DisplayName("无尝试记录时应通过")
        void shouldPassWhenNoAttempts() {
            when(loginAttemptPort.getAttempts("testuser")).thenReturn(null);

            loginSecurityDomainService.checkLoginAttempts("testuser");
        }

        @Test
        @DisplayName("尝试次数未达上限时应通过")
        void shouldPassWhenBelowMaxAttempts() {
            when(loginAttemptPort.getAttempts("testuser")).thenReturn(3L);

            loginSecurityDomainService.checkLoginAttempts("testuser");
        }

        @Test
        @DisplayName("尝试次数达到上限时应抛出异常")
        void shouldThrowWhenMaxAttemptsReached() {
            when(loginAttemptPort.getAttempts("testuser"))
                .thenReturn((long) LoginCacheConstants.MAX_LOGIN_ATTEMPTS);

            assertThatThrownBy(() -> loginSecurityDomainService.checkLoginAttempts("testuser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("尝试次数超过上限时应抛出异常")
        void shouldThrowWhenExceedsMaxAttempts() {
            when(loginAttemptPort.getAttempts("testuser")).thenReturn(10L);

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
            when(loginAttemptPort.incrementAttempts("testuser")).thenReturn(1L);

            loginSecurityDomainService.recordFailedAttempt("testuser");

            verify(loginAttemptPort).incrementAttempts("testuser");
            verify(loginAttemptPort).expireAttempts(eq("testuser"), eq(LoginCacheConstants.ATTEMPTS_EXPIRE_TIME));
        }

        @Test
        @DisplayName("达到最大尝试次数时应抛出异常")
        void shouldThrowWhenMaxReached() {
            when(loginAttemptPort.incrementAttempts("testuser"))
                .thenReturn((long) LoginCacheConstants.MAX_LOGIN_ATTEMPTS);

            assertThatThrownBy(() -> loginSecurityDomainService.recordFailedAttempt("testuser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("未达最大尝试次数时不应抛出异常")
        void shouldNotThrowWhenBelowMax() {
            when(loginAttemptPort.incrementAttempts("testuser")).thenReturn(2L);

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
    }

    @Nested
    @DisplayName("clearLoginAttempts")
    class ClearLoginAttemptsTests {

        @Test
        @DisplayName("应清除尝试记录")
        void shouldClearAttempts() {
            loginSecurityDomainService.clearLoginAttempts("testuser");

            verify(loginAttemptPort).clearAttempts("testuser");
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
            String result = loginSecurityDomainService.maskAccount("test@example.com");

            assertThat(result).isNotNull();
            assertThat(result).contains("****");
            assertThat(result).contains("@example.com");
        }

        @Test
        @DisplayName("应正确脱敏手机号")
        void shouldMaskPhone() {
            String result = loginSecurityDomainService.maskAccount("13812345678");

            assertThat(result).isNotNull();
            assertThat(result).contains("****");
            assertThat(result).startsWith("138");
            assertThat(result).endsWith("5678");
        }

        @Test
        @DisplayName("普通用户名应原样返回")
        void shouldReturnPlainUsername() {
            String result = loginSecurityDomainService.maskAccount("testuser");

            assertThat(result).isEqualTo("testuser");
        }

        @Test
        @DisplayName("null 应返回 null")
        void shouldReturnNullForNull() {
            String result = loginSecurityDomainService.maskAccount(null);

            assertThat(result).isNull();
        }
    }
}
