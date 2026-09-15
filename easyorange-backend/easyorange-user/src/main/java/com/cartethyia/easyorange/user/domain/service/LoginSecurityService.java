package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginAttemptPort loginAttemptPort;

    public void checkAndThrowIfLocked(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        if (loginAttemptPort.getRemainingLockSeconds(identifier) > 0) {
            throw BusinessException.of(UserResultCode.USER_LOCKED);
        }
    }

    public void incrementAndCheck(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long count = loginAttemptPort.incrementAndGet(identifier, UserSecurityConstant.LOCK_DURATION);
        if (count >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of(UserResultCode.USER_LOCKED);
        }
    }

    public void clear(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        loginAttemptPort.clear(identifier);
    }
}
