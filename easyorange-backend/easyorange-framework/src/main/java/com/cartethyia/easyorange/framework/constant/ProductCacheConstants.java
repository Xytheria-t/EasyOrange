package com.cartethyia.easyorange.framework.constant;

public class ProductCacheConstants {

    private static final String APP_PREFIX = "eo:";

    public static final String INFO_KEY = APP_PREFIX + "product:info:";
    public static final String LIST_KEY = APP_PREFIX + "product:list:";
    public static final long INFO_EXPIRE_TIME = 60L;
    public static final long LIST_EXPIRE_TIME = 30L;

    private ProductCacheConstants() {
    }

    public static String infoKey(Object id) {
        return INFO_KEY + id;
    }

    public static String listKey(Object categoryId) {
        return LIST_KEY + categoryId;
    }
}