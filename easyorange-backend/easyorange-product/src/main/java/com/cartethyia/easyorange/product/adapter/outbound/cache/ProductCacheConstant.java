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

    /** 商品详情缓存（Spring Cache cacheName，Redis key 形如 {@code eo:product:info::<id>}） */
    public static final String PRODUCT_INFO_CACHE = PRODUCT_PREFIX + "info";
    /** 分类列表缓存（Spring Cache cacheName，key 形如 {@code level:1} / {@code parent:<parentId>}） */
    public static final String CATEGORY_LIST_CACHE = APP_PREFIX + "category:list";

    public static String searchHistoryKey(String userId) {
        return SEARCH_HISTORY_KEY_PREFIX + userId;
    }
}
