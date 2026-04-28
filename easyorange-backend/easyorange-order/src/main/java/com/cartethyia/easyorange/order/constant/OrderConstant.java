package com.cartethyia.easyorange.order.constant;

import com.cartethyia.easyorange.common.util.BizRequire;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderConstant {

    // ==================== 应用前缀 ====================
    private static final String APP_PREFIX = "eo:";
    private static final String ORDER_PREFIX = APP_PREFIX + "order:";

    // ==================== 订单号生成常量 ====================
    public static final String ORDER_NO_PREFIX = "ORD";
    public static final long ORDER_TWEPOCH = 1704067200000L;
    public static final int ORDER_WORKER_ID_BITS = 5;
    public static final int ORDER_DATACENTER_ID_BITS = 5;
    public static final int ORDER_SEQUENCE_BITS = 12;
    public static final long ORDER_MAX_WORKER_ID = ~(-1L << ORDER_WORKER_ID_BITS);
    public static final long ORDER_MAX_DATACENTER_ID = ~(-1L << ORDER_DATACENTER_ID_BITS);
    public static final int ORDER_WORKER_ID_SHIFT = ORDER_SEQUENCE_BITS;
    public static final int ORDER_DATACENTER_ID_SHIFT = ORDER_SEQUENCE_BITS + ORDER_WORKER_ID_BITS;
    public static final int ORDER_TIMESTAMP_LEFT_SHIFT = ORDER_SEQUENCE_BITS + ORDER_WORKER_ID_BITS + ORDER_DATACENTER_ID_BITS;
    public static final long ORDER_SEQUENCE_MASK = ~(-1L << ORDER_SEQUENCE_BITS);

    // ==================== 订单业务常量 ====================
    public static final long AUTO_CONFIRM_DAYS = 7;

    // ==================== 订单缓存 Key ====================
    public static final String ORDER_DETAIL_KEY = ORDER_PREFIX + "detail:";
    public static final long ORDER_DETAIL_EXPIRE_TIME = 30L;

    // ==================== Key 生成方法 ====================
    public static String orderDetailKey(Long orderId) {
        BizRequire.notNull(orderId, "orderId 不能为 null");
        return ORDER_DETAIL_KEY + orderId;
    }
}
