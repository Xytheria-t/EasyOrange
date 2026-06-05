package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginAttemptPort loginAttemptPort;

    /**
     * Checks whether the login attempts for the given identifier exceed the maximum threshold.
     *
     * @param identifier the login identifier (username, email, or phone)
     * @throws BusinessException if the maximum number of failed login attempts has been reached
     */
    public void checkLoginAttempts(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        Long attempts = loginAttemptPort.countAttempts(identifier);
        if (attempts != null && attempts >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of(lockedMessage(identifier));
        }
    }

    /**
     * Records a failed login attempt and locks the account if the threshold is reached.
     *
     * @param identifier the login identifier (username, email, or phone)
     * @throws BusinessException if the maximum number of failed attempts is reached after incrementing
     */
    public void recordFailedAttempt(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long count = loginAttemptPort.incrementAttempts(identifier, UserSecurityConstant.ATTEMPTS_EXPIRE_TIME);
        if (count >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of(lockedMessage(identifier));
        }
    }

    /**
     * Clears all recorded login attempts for the given identifier (called after a successful login).
     *
     * @param identifier the login identifier (username, email, or phone)
     */
    public void clearLoginAttempts(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        loginAttemptPort.clearAttempts(identifier);
    }

    private String lockedMessage(String identifier) {
        long seconds = loginAttemptPort.getRemainingLockSeconds(identifier);
        return "登录失败次数过多，账户已锁定，" + Math.max(1, (seconds + 59) / 60) + "分钟后可重试";
    }
}
