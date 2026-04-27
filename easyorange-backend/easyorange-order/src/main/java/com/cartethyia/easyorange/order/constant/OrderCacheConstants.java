package com.cartethyia.easyorange.order.constant;

import com.cartethyia.easyorange.common.util.BizRequire;

public class OrderCacheConstants {

    private static final String APP_PREFIX = "eo:";

    public static final String DETAIL_KEY = APP_PREFIX + "order:detail:";
    public static final long DETAIL_EXPIRE_TIME = 30L;

    private OrderCacheConstants() {
    }

    public static String detailKey(Long orderId) {
        BizRequire.notNull(orderId, "orderId 不能为 null");
        return DETAIL_KEY + orderId;
    }
}