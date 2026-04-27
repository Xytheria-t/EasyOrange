package com.cartethyia.easyorange.framework.constant;

import com.cartethyia.easyorange.common.util.BizRequire;

public class LoginCacheConstants {

    public static final String APP_PREFIX = "eo:";
    public static final String TOKEN_KEY = APP_PREFIX + "login_tokens:";
    public static final String ATTEMPTS_KEY = APP_PREFIX + "login:attempts:";
    public static final long ATTEMPTS_EXPIRE_TIME = 30L;

    private LoginCacheConstants() {
    }

    public static String tokenKey(String token) {
        BizRequire.notNull(token, "token 不能为 null");
        return TOKEN_KEY + token;
    }

    public static String attemptsKey(String username) {
        BizRequire.notNull(username, "username 不能为 null");
        return ATTEMPTS_KEY + username;
    }
}