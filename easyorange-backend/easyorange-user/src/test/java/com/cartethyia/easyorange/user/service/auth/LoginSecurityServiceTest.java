package com.cartethyia.easyorange.user.service.auth;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.service.auth.impl.LoginSecurityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSecurityServiceTest {

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private LoginSecurityServiceImpl loginSecurityService;

    @Test
    void checkLoginAttempts_passesWhenNoAttempts() {
        when(redisCache.get(any(), eq(Long.class))).thenReturn(null);

        assertDoesNotThrow(() -> loginSecurityService.checkLoginAttempts("testuser"));
    }

    @Test
    void checkLoginAttempts_passesWhenBelowMaxAttempts() {
        when(redisCache.get(any(), eq(Long.class))).thenReturn(3L);

        assertDoesNotThrow(() -> loginSecurityService.checkLoginAttempts("testuser"));
    }

    @Test
    void checkLoginAttempts_throwsWhenMaxAttemptsReached() {
        when(redisCache.get(any(), eq(Long.class))).thenReturn((long) LoginCacheConstants.MAX_LOGIN_ATTEMPTS);

        assertThrows(BusinessException.class, () -> loginSecurityService.checkLoginAttempts("testuser"));
    }

    @Test
    void checkLoginAttempts_throwsWhenBlankAccount() {
        assertThrows(BusinessException.class, () -> loginSecurityService.checkLoginAttempts(""));
        assertThrows(BusinessException.class, () -> loginSecurityService.checkLoginAttempts(null));
    }

    @Test
    void recordFailedAttempt_incrementsCounter() {
        when(redisCache.increment(any())).thenReturn(1L);

        loginSecurityService.recordFailedAttempt("testuser");

        verify(redisCache).expire(any(), eq(LoginCacheConstants.ATTEMPTS_EXPIRE_TIME), eq(TimeUnit.MINUTES));
    }

    @Test
    void recordFailedAttempt_throwsWhenMaxAttemptsReached() {
        when(redisCache.increment(any())).thenReturn((long) LoginCacheConstants.MAX_LOGIN_ATTEMPTS);

        assertThrows(BusinessException.class, () -> loginSecurityService.recordFailedAttempt("testuser"));
    }

    @Test
    void recordFailedAttempt_throwsWhenBlankAccount() {
        assertThrows(BusinessException.class, () -> loginSecurityService.recordFailedAttempt(""));
    }

    @Test
    void clearLoginAttempts_deletesKey() {
        loginSecurityService.clearLoginAttempts("testuser");

        verify(redisCache).delete((String) any());
    }

    @Test
    void clearLoginAttempts_throwsWhenBlankAccount() {
        assertThrows(BusinessException.class, () -> loginSecurityService.clearLoginAttempts(""));
    }

    @Test
    void maskAccount_masksEmail() {
        String result = loginSecurityService.maskAccount("test@example.com");

        assertNotNull(result);
        assertTrue(result.contains("***"));
    }

    @Test
    void maskAccount_masksPhone() {
        String result = loginSecurityService.maskAccount("13812345678");

        assertNotNull(result);
        assertTrue(result.contains("***"));
    }

    @Test
    void maskAccount_returnsPlainUsername() {
        String result = loginSecurityService.maskAccount("testuser");

        assertEquals("testuser", result);
    }

    @Test
    void maskAccount_returnsNullForNull() {
        assertNull(loginSecurityService.maskAccount(null));
    }
}