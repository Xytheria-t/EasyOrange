package com.cartethyia.easyorange.framework.constant;

import com.cartethyia.easyorange.common.util.BizRequire;

public class RateLimitCacheConstants {

    private static final String APP_PREFIX = "eo:";

    public static final String REPEAT_SUBMIT_KEY = APP_PREFIX + "repeat:submit:";
    public static final String KEY = APP_PREFIX + "rate:limit:";

    private RateLimitCacheConstants() {
    }

    public static String repeatSubmitKey(String token, String methodKey, String paramsHash) {
        BizRequire.notNull(token, "repeatSubmitKey 参数 token 不能为 null");
        BizRequire.notNull(methodKey, "repeatSubmitKey 参数 methodKey 不能为 null");
        BizRequire.notNull(paramsHash, "repeatSubmitKey 参数 paramsHash 不能为 null");
        return REPEAT_SUBMIT_KEY + token + ":" + methodKey + ":" + paramsHash;
    }

    public static String key(String type, String key) {
        BizRequire.notNull(type, "rateLimitKey 参数 type 不能为 null");
        BizRequire.notNull(key, "rateLimitKey 参数 key 不能为 null");
        return KEY + type + ":" + key;
    }
}