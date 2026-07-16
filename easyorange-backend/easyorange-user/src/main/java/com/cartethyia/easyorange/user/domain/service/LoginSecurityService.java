package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.exception.AccountLockedException;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginAttemptPort loginAttemptPort;

    /**
     * 预检 — 若账户已锁定则直接抛出 {@link AccountLockedException}。
     * 用于密码登录场景中，在数据库查询前快速失败。
     */
    public void checkAndThrowIfLocked(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long remaining = loginAttemptPort.getRemainingLockSeconds(identifier);
        if (remaining > 0) {
            throw AccountLockedException.of(identifier, remaining);
        }
    }

    /**
     * 递增失败次数；若达到阈值则抛出 {@link AccountLockedException}。
     */
    public void incrementAndCheck(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long count = loginAttemptPort.incrementAndGet(identifier, UserSecurityConstant.LOCK_DURATION);
        if (count >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            long remaining = loginAttemptPort.getRemainingLockSeconds(identifier);
            throw AccountLockedException.of(identifier, remaining);
        }
    }

    /**
     * 清除登录失败记录（登录成功后调用）。
     */
    public void clear(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        loginAttemptPort.clear(identifier);
    }
}
