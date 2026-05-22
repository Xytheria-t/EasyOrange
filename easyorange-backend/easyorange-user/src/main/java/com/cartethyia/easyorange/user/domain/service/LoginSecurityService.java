package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.output.LoginAttemptPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginSecurityService {

    private final LoginAttemptPort loginAttemptPort;

    public void checkLoginAttempts(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        Long attempts = loginAttemptPort.getAttempts(account);
        if (attempts != null && attempts >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of("登录失败次数过多，账户已锁定" + UserSecurityConstant.LOGIN_LOCK_MINUTES + "分钟");
        }
    }

    public void recordFailedAttempt(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        long count = loginAttemptPort.incrementAndExpire(account, UserSecurityConstant.ATTEMPTS_EXPIRE_TIME);
        if (count >= UserSecurityConstant.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of("登录失败次数过多，账户已锁定" + UserSecurityConstant.LOGIN_LOCK_MINUTES + "分钟");
        }
    }

    public void clearLoginAttempts(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        loginAttemptPort.clearAttempts(account);
    }
}
