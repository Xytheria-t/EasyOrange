package com.cartethyia.easyorange.product.constant;

public class ProductConstants {

    private static final String APP_PREFIX = "eo:";

    private ProductConstants() {}

    public static final int CONDITION_LEVEL_NEW = 1;
    public static final int CONDITION_LEVEL_LIKE_NEW = 2;
    public static final int CONDITION_LEVEL_GOOD = 3;
    public static final int CONDITION_LEVEL_FAIR = 4;

    public static final String HOT_KEYWORD_ZSET_KEY = APP_PREFIX + "search:hot:zset";
    public static final String SEARCH_HISTORY_KEY_PREFIX = APP_PREFIX + "search:history:user:";
    public static final long SEARCH_HISTORY_EXPIRE_DAYS = 7L;
    public static final int HOT_KEYWORD_LIMIT = 50;

    public static final int HOT_LEVEL_5_THRESHOLD = 1000;
    public static final int HOT_LEVEL_4_THRESHOLD = 500;
    public static final int HOT_LEVEL_3_THRESHOLD = 200;
    public static final int HOT_LEVEL_2_THRESHOLD = 100;
    public static final int HOT_LEVEL_1_THRESHOLD = 50;

    public static final String CRON_SYNC_HOT_KEYWORDS = "0 0/30 * * * ?";
    public static final String CRON_CLEANUP_SEARCH_HISTORY = "0 0 2 * * ?";

    public static final int IMAGE_IS_MAIN = 1;
    public static final int IMAGE_NOT_MAIN = 0;
}