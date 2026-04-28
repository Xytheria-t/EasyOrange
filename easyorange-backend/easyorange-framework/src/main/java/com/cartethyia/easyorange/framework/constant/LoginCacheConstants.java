package com.cartethyia.easyorange.framework.constant;

import com.cartethyia.easyorange.common.util.BizRequire;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoginCacheConstants {

    // ==================== 缓存前缀 ====================
    public static final String APP_PREFIX = "eo:";
    private static final String USER_PREFIX = APP_PREFIX + "user:";

    // ==================== Token 缓存 Key ====================
    public static final String TOKEN_KEY = USER_PREFIX + "token:";
    public static final String ATTEMPTS_KEY = USER_PREFIX + "login:attempts:";
    public static final long ATTEMPTS_EXPIRE_TIME = 30L;

    // ==================== 登录安全常量 ====================
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_LOCK_MINUTES = 30;

    // ==================== 验证常量 ====================
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 20;
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 20;
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$";
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    public static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    // ==================== Key 生成方法 ====================
    public static String buildTokenKey(String token) {
        BizRequire.notNull(token, "token 不能为 null");
        return TOKEN_KEY + token;
    }

    public static String buildAttemptsKey(String username) {
        BizRequire.notNull(username, "username 不能为 null");
        return ATTEMPTS_KEY + username;
    }
}
