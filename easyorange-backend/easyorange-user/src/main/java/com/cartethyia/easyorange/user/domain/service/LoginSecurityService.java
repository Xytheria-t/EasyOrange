package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginAttemptPort loginAttemptPort;

    public void checkLoginAttempts(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        Long attempts = loginAttemptPort.getAttempts(identifier);
        if (attempts != null && attempts >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of(lockedMessage());
        }
    }

    public void recordFailedAttempt(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        long count = loginAttemptPort.incrementAndExpire(identifier, UserSecurityConstant.ATTEMPTS_EXPIRE_TIME);
        if (count >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of(lockedMessage());
        }
    }

    public void clearLoginAttempts(String identifier) {
        BizRequire.notBlank(identifier, "登录标识不能为空");
        loginAttemptPort.clearAttempts(identifier);
    }

    private static String lockedMessage() {
        return "登录失败次数过多，账户已锁定" + UserSecurityConstant.LOGIN_LOCK_MINUTES + "分钟";
    }
}
