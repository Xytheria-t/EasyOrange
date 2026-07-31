package com.cartethyia.easyorange.product.adapter.outbound.cache;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductCacheConstant {

    private static final String APP_PREFIX = "eo:";
    private static final String PRODUCT_PREFIX = APP_PREFIX + "product:";

    public static final String HOT_KEYWORD_ZSET_KEY = APP_PREFIX + "search:hot:zset";
    public static final String SEARCH_HISTORY_KEY_PREFIX = APP_PREFIX + "search:history:user:";
    public static final int SEARCH_HISTORY_MAX_SIZE = 20;

    public static final String PRODUCT_INFO_KEY = PRODUCT_PREFIX + "info:";
    public static final String PRODUCT_LIST_KEY = PRODUCT_PREFIX + "list:";

    public static String infoKey(Object id) {
        return PRODUCT_INFO_KEY + id;
    }

    public static String listKey(Object categoryId) {
        return PRODUCT_LIST_KEY + categoryId;
    }

    public static String searchHistoryKey(String userId) {
        return SEARCH_HISTORY_KEY_PREFIX + userId;
    }
}
