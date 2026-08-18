package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.exception.AccountLockedException;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginAttemptPort loginAttemptPort;

    public void checkAndThrowIfLocked(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long remaining = loginAttemptPort.getRemainingLockSeconds(identifier);
        if (remaining > 0) {
            throw AccountLockedException.of(identifier, remaining);
        }
    }

    public void incrementAndCheck(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long count = loginAttemptPort.incrementAndGet(identifier, UserSecurityConstant.LOCK_DURATION);
        if (count >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            long remaining = loginAttemptPort.getRemainingLockSeconds(identifier);
            throw AccountLockedException.of(identifier, remaining);
        }
    }

    public void clear(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        loginAttemptPort.clear(identifier);
    }
}
