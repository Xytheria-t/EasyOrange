package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.exception.AccountLockedException;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
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
    @DisplayName("checkAndThrowIfLocked")
    class CheckAndThrowIfLockedTests {

        @Test
        @DisplayName("账户未锁定时不抛出异常")
        void notLocked() {
            when(loginAttemptPort.getRemainingLockSeconds(ACCOUNT)).thenReturn(0L);

            assertThatCode(() -> service.checkAndThrowIfLocked(ACCOUNT))
                .doesNotThrowAnyException();

            verify(loginAttemptPort).getRemainingLockSeconds(ACCOUNT);
        }

        @Test
        @DisplayName("账户锁定超过阈值时抛出 AccountLockedException")
        void locked() {
            when(loginAttemptPort.getRemainingLockSeconds(ACCOUNT)).thenReturn(600L);

            assertThatThrownBy(() -> service.checkAndThrowIfLocked(ACCOUNT))
                .isInstanceOf(AccountLockedException.class);

            verify(loginAttemptPort).getRemainingLockSeconds(ACCOUNT);
        }

        @Test
        @DisplayName("登录标识为空时抛出异常")
        void blankIdentifier() {
            assertThatThrownBy(() -> service.checkAndThrowIfLocked(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录标识不能为空");

            verify(loginAttemptPort, never()).getRemainingLockSeconds(any());
        }
    }

    @Nested
    @DisplayName("incrementAndCheck")
    class IncrementAndCheckTests {

        @Test
        @DisplayName("记录失败尝试未达上限时不抛出异常")
        void belowMax() {
            when(loginAttemptPort.incrementAndGet(ACCOUNT, UserSecurityConstant.LOCK_DURATION)).thenReturn(3L);

            assertThatCode(() -> service.incrementAndCheck(ACCOUNT))
                .doesNotThrowAnyException();

            verify(loginAttemptPort).incrementAndGet(ACCOUNT, UserSecurityConstant.LOCK_DURATION);
        }

        @Test
        @DisplayName("达到上限时抛出 AccountLockedException")
        void reachedMax() {
            when(loginAttemptPort.incrementAndGet(ACCOUNT, UserSecurityConstant.LOCK_DURATION))
                .thenReturn((long) UserSecurityConstant.MAX_LOGIN_ATTEMPTS);
            when(loginAttemptPort.getRemainingLockSeconds(ACCOUNT)).thenReturn(1800L);

            assertThatThrownBy(() -> service.incrementAndCheck(ACCOUNT))
                .isInstanceOf(AccountLockedException.class);

            verify(loginAttemptPort).incrementAndGet(ACCOUNT, UserSecurityConstant.LOCK_DURATION);
            verify(loginAttemptPort).getRemainingLockSeconds(ACCOUNT);
        }

        @Test
        @DisplayName("登录标识为空时抛出异常")
        void blankIdentifier() {
            assertThatThrownBy(() -> service.incrementAndCheck(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录标识不能为空");

            verify(loginAttemptPort, never()).incrementAndGet(any(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearTests {

        @Test
        @DisplayName("清除登录尝试成功")
        void success() {
            service.clear(ACCOUNT);

            verify(loginAttemptPort).clear(ACCOUNT);
        }

        @Test
        @DisplayName("登录标识为空时抛出异常")
        void blankIdentifier() {
            assertThatThrownBy(() -> service.clear(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录标识不能为空");

            verify(loginAttemptPort, never()).clear(any());
        }
    }
}
