package com.cartethyia.easyorange.order.adapter.outbound.cache;

import com.cartethyia.easyorange.common.util.BizRequire;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderCacheConstant {

    private static final String APP_PREFIX = "eo:";
    private static final String ORDER_PREFIX = APP_PREFIX + "order:";

    public static final String ORDER_DETAIL_KEY = ORDER_PREFIX + "detail:";
    public static final long ORDER_DETAIL_EXPIRE_TIME = 30L;

    public static final String ORDER_LIST_CACHE_KEY_PREFIX = ORDER_PREFIX + "list:";
    public static final String ORDER_DETAIL_CACHE_KEY_PREFIX = ORDER_DETAIL_KEY;
    public static final long ORDER_LIST_CACHE_EXPIRE_MINUTES = 30;
    public static final long ORDER_DETAIL_CACHE_EXPIRE_MINUTES = 60;

    public static String orderDetailKey(Long orderId) {
        BizRequire.notNull(orderId, "orderId 不能为 null");
        return ORDER_DETAIL_KEY + orderId;
    }
}