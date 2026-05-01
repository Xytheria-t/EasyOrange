package com.cartethyia.easyorange.common.constant;

import com.cartethyia.easyorange.common.util.BizRequire;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConstant {

    public static final String APP_PREFIX = "eo:";

    public static final String CACHE_PREFIX = APP_PREFIX + "cache:";
    public static final long DEFAULT_CACHE_TTL = 60L;

    public static final String RATE_LIMIT_PREFIX = APP_PREFIX + "rate:limit:";
    public static final String REPEAT_SUBMIT_PREFIX = APP_PREFIX + "repeat:submit:";

    public static final int FILE_STATUS_NORMAL = 1;

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public static String rateLimitKey(String type, String key) {
        BizRequire.notNull(type, "rateLimitKey 参数 type 不能为 null");
        BizRequire.notNull(key, "rateLimitKey 参数 key 不能为 null");
        return RATE_LIMIT_PREFIX + type + ":" + key;
    }

    public static String repeatSubmitKey(String token, String methodKey, String paramsHash) {
        BizRequire.notNull(token, "repeatSubmitKey 参数 token 不能为 null");
        BizRequire.notNull(methodKey, "repeatSubmitKey 参数 methodKey 不能为 null");
        BizRequire.notNull(paramsHash, "repeatSubmitKey 参数 paramsHash 不能为 null");
        return REPEAT_SUBMIT_PREFIX + token + ":" + methodKey + ":" + paramsHash;
    }
}
