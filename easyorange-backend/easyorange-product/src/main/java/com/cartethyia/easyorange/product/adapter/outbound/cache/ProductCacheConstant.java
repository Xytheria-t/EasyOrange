package com.cartethyia.easyorange.product.adapter.outbound.cache;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductCacheConstant {

    private static final String APP_PREFIX = "eo:";
    private static final String PRODUCT_PREFIX = APP_PREFIX + "product:";
    private static final String CATEGORY_PREFIX = APP_PREFIX + "category:";

    public static final String HOT_KEYWORD_ZSET_KEY = APP_PREFIX + "search:hot:zset";
    public static final String SEARCH_HISTORY_KEY_PREFIX = APP_PREFIX + "search:history:user:";
    public static final long SEARCH_HISTORY_EXPIRE_DAYS = 7L;
    public static final int HOT_KEYWORD_LIMIT = 50;

    public static final String PRODUCT_INFO_KEY = PRODUCT_PREFIX + "info:";
    public static final String PRODUCT_LIST_KEY = PRODUCT_PREFIX + "list:";
    public static final long PRODUCT_INFO_EXPIRE_TIME = 60L;
    public static final long PRODUCT_LIST_EXPIRE_TIME = 30L;

    public static final String CATEGORY_LIST_KEY = CATEGORY_PREFIX + "list";
    public static final long CATEGORY_INFO_EXPIRE_TIME = 120L;

    public static final String PRODUCT_BLOOM_KEY = PRODUCT_PREFIX + "bloom:id";

    public static final String CRON_SYNC_HOT_KEYWORDS = "0 0/30 * * * ?";
    public static final String CRON_CLEANUP_SEARCH_HISTORY = "0 0 2 * * ?";

    public static String infoKey(Object id) {
        return PRODUCT_INFO_KEY + id;
    }

    public static String listKey(Object categoryId) {
        return PRODUCT_LIST_KEY + categoryId;
    }

    public static String searchHistoryKey(Long userId) {
        return SEARCH_HISTORY_KEY_PREFIX + userId;
    }
}
