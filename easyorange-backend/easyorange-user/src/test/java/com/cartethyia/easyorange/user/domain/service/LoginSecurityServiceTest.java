package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginSecurityService 测试")
class LoginSecurityServiceTest {

    @Mock
    private LoginAttemptPort loginAttemptPort;

    private LoginSecurityService service;

    private static final String ACCOUNT = "testuser";

    @BeforeEach
    void setUp() {
        service = new LoginSecurityService(loginAttemptPort);
    }

    @Nested
    @DisplayName("checkLoginAttempts")
    class CheckLoginAttemptsTests {

        @Test
        @DisplayName("未超过尝试次数时不抛出异常")
        void belowMaxAttempts() {
            when(loginAttemptPort.countAttempts(ACCOUNT)).thenReturn(3L);

            service.checkLoginAttempts(ACCOUNT);

            verify(loginAttemptPort).countAttempts(ACCOUNT);
        }

        @Test
        @DisplayName("超过最大尝试次数时抛出异常")
        void exceededMaxAttempts() {
            when(loginAttemptPort.countAttempts(ACCOUNT)).thenReturn((long) UserSecurityConstant.MAX_LOGIN_ATTEMPTS);
            when(loginAttemptPort.getRemainingLockSeconds(ACCOUNT)).thenReturn(600L);

            assertThatThrownBy(() -> service.checkLoginAttempts(ACCOUNT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("没有尝试记录时不抛出异常")
        void noAttempts() {
            when(loginAttemptPort.countAttempts(ACCOUNT)).thenReturn(null);

            service.checkLoginAttempts(ACCOUNT);

            verify(loginAttemptPort).countAttempts(ACCOUNT);
        }

        @Test
        @DisplayName("登录标识为空时抛出异常")
        void blankIdentifier() {
            assertThatThrownBy(() -> service.checkLoginAttempts(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录标识不能为空");

            verify(loginAttemptPort, never()).countAttempts(any());
        }
    }

    @Nested
    @DisplayName("recordFailedAttempt")
    class RecordFailedAttemptTests {

        @Test
        @DisplayName("记录失败尝试未达上限时不抛出异常")
        void belowMax() {
            when(loginAttemptPort.incrementAttempts(ACCOUNT, UserSecurityConstant.ATTEMPTS_EXPIRE_TIME)).thenReturn(3L);

            service.recordFailedAttempt(ACCOUNT);

            verify(loginAttemptPort).incrementAttempts(ACCOUNT, UserSecurityConstant.ATTEMPTS_EXPIRE_TIME);
        }

        @Test
        @DisplayName("达到上限时抛出锁定异常")
        void reachedMax() {
            when(loginAttemptPort.incrementAttempts(ACCOUNT, UserSecurityConstant.ATTEMPTS_EXPIRE_TIME))
                .thenReturn((long) UserSecurityConstant.MAX_LOGIN_ATTEMPTS);
            when(loginAttemptPort.getRemainingLockSeconds(ACCOUNT)).thenReturn(1800L);

            assertThatThrownBy(() -> service.recordFailedAttempt(ACCOUNT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        }

        @Test
        @DisplayName("登录标识为空时抛出异常")
        void blankIdentifier() {
            assertThatThrownBy(() -> service.recordFailedAttempt(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录标识不能为空");

            verify(loginAttemptPort, never()).incrementAttempts(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("clearLoginAttempts")
    class ClearLoginAttemptsTests {

        @Test
        @DisplayName("清除登录尝试成功")
        void success() {
            service.clearLoginAttempts(ACCOUNT);

            verify(loginAttemptPort).clearAttempts(ACCOUNT);
        }

        @Test
        @DisplayName("登录标识为空时抛出异常")
        void blankIdentifier() {
            assertThatThrownBy(() -> service.clearLoginAttempts(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录标识不能为空");

            verify(loginAttemptPort, never()).clearAttempts(any());
        }
    }
}
