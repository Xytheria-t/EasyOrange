package com.cartethyia.easyorange.user.service.impl;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.constant.UserConstants;
import com.cartethyia.easyorange.user.service.LoginSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private static final Pattern PHONE_PATTERN = Pattern.compile(UserConstants.PHONE_REGEX);

    private final RedisCache redisCache;

    public LoginSecurityServiceImpl(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    public void checkLoginAttempts(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        String key = LoginCacheConstants.attemptsKey(account);
        Long attempts = redisCache.get(key, Long.class);
        if (attempts != null && attempts >= UserConstants.MAX_LOGIN_ATTEMPTS) {
            throw BusinessException.of("登录失败次数过多，账户已锁定" + UserConstants.LOGIN_LOCK_MINUTES + "分钟");
        }
    }

    @Override
    public void recordFailedAttempt(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        String key = LoginCacheConstants.attemptsKey(account);
        Long count = redisCache.increment(key);
        if (count != null && count == 1) {
            redisCache.expire(key, LoginCacheConstants.ATTEMPTS_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        log.warn("action=login_fail, account={}, attempts={}/{}", maskAccount(account), count, UserConstants.MAX_LOGIN_ATTEMPTS);
    }

    @Override
    public void clearLoginAttempts(String account) {
        BizRequire.notBlank(account, "账号不能为空");
        String key = LoginCacheConstants.attemptsKey(account);
        redisCache.delete(key);
    }

    @Override
    public String maskAccount(String account) {
        if (account == null) {
            return null;
        }
        if (account.contains("@")) {
            return MaskUtils.maskEmail(account);
        }
        if (PHONE_PATTERN.matcher(account).matches()) {
            return MaskUtils.maskPhone(account);
        }
        return account;
    }
}