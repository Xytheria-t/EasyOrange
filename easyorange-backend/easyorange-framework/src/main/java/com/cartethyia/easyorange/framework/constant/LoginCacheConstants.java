package com.cartethyia.easyorange.framework.constant;

import com.cartethyia.easyorange.common.util.BizRequire;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoginCacheConstants {

    public static final String APP_PREFIX = "eo:";
    public static final String USER_PREFIX = APP_PREFIX + "user:";

    public static final String TOKEN_BLACKLIST_KEY = USER_PREFIX + "token:blacklist:";
    public static final String FORCE_LOGOUT_KEY = USER_PREFIX + "token:force-logout:";
    public static final String ATTEMPTS_KEY = USER_PREFIX + "login:attempts:";

    public static String buildAttemptsKey(String identifier) {
        BizRequire.notNull(identifier, "identifier 不能为 null");
        return ATTEMPTS_KEY + identifier;
    }
}
