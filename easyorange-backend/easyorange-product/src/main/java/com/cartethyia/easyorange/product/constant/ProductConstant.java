package com.cartethyia.easyorange.product.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductConstant {

    // ==================== 应用前缀 ====================
    private static final String APP_PREFIX = "eo:";
    private static final String PRODUCT_PREFIX = APP_PREFIX + "product:";
    private static final String CATEGORY_PREFIX = APP_PREFIX + "category:";

    // ==================== 商品状态常量 ====================
    public static final int CONDITION_LEVEL_NEW = 1;
    public static final int CONDITION_LEVEL_LIKE_NEW = 2;
    public static final int CONDITION_LEVEL_GOOD = 3;
    public static final int CONDITION_LEVEL_FAIR = 4;

    // ==================== 图片常量 ====================
    public static final int IMAGE_IS_MAIN = 1;
    public static final int IMAGE_NOT_MAIN = 0;

    // ==================== 搜索相关常量 ====================
    public static final String HOT_KEYWORD_ZSET_KEY = APP_PREFIX + "search:hot:zset";
    public static final String SEARCH_HISTORY_KEY_PREFIX = APP_PREFIX + "search:history:user:";
    public static final long SEARCH_HISTORY_EXPIRE_DAYS = 7L;
    public static final int HOT_KEYWORD_LIMIT = 50;

    // ==================== 热度等级阈值 ====================
    public static final int HOT_LEVEL_5_THRESHOLD = 1000;
    public static final int HOT_LEVEL_4_THRESHOLD = 500;
    public static final int HOT_LEVEL_3_THRESHOLD = 200;
    public static final int HOT_LEVEL_2_THRESHOLD = 100;
    public static final int HOT_LEVEL_1_THRESHOLD = 50;

    // ==================== 定时任务 Cron 表达式 ====================
    public static final String CRON_SYNC_HOT_KEYWORDS = "0 0/30 * * * ?";
    public static final String CRON_CLEANUP_SEARCH_HISTORY = "0 0 2 * * ?";

    // ==================== 商品缓存 Key ====================
    public static final String PRODUCT_INFO_KEY = PRODUCT_PREFIX + "info:";
    public static final String PRODUCT_LIST_KEY = PRODUCT_PREFIX + "list:";
    public static final long PRODUCT_INFO_EXPIRE_TIME = 60L;
    public static final long PRODUCT_LIST_EXPIRE_TIME = 30L;

    // ==================== 分类缓存 Key ====================
    public static final String CATEGORY_LIST_KEY = CATEGORY_PREFIX + "list";
    public static final long CATEGORY_INFO_EXPIRE_TIME = 120L;

    // ==================== Key 生成方法 ====================
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
