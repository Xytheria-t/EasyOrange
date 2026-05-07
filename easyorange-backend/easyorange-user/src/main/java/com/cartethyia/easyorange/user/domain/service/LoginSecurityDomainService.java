package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.user.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoginSecurityDomainService {

    private final LoginAttemptPort loginAttemptPort;

    public void checkLoginAttempts(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        Long attempts = loginAttemptPort.getAttempts(account);
        if (attempts != null && attempts >= LoginCacheConstants.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of("登录失败次数过多，账户已锁定" + LoginCacheConstants.LOGIN_LOCK_MINUTES + "分钟");
        }
    }

    public void recordFailedAttempt(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        long count = loginAttemptPort.incrementAttempts(account);
        loginAttemptPort.expireAttempts(account, LoginCacheConstants.ATTEMPTS_EXPIRE_TIME);
        if (count >= LoginCacheConstants.MAX_LOGIN_ATTEMPTS) {
            log.warn("action=account_locked, account={}, attempts={}/{}", maskAccount(account), count, LoginCacheConstants.MAX_LOGIN_ATTEMPTS);
            throw BusinessException.of("登录失败次数过多，账户已锁定" + LoginCacheConstants.LOGIN_LOCK_MINUTES + "分钟");
        }
        log.warn("action=login_fail, account={}, attempts={}/{}", maskAccount(account), count, LoginCacheConstants.MAX_LOGIN_ATTEMPTS);
    }

    public void clearLoginAttempts(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        loginAttemptPort.clearAttempts(account);
    }

    public String maskAccount(String account) {
        if (account == null) {
            return null;
        }
        if (UserConstant.EMAIL_PATTERN.matcher(account).matches()) {
            return MaskUtils.maskEmail(account);
        }
        if (UserConstant.PHONE_PATTERN.matcher(account).matches()) {
            return MaskUtils.maskPhone(account);
        }
        return account;
    }
}
