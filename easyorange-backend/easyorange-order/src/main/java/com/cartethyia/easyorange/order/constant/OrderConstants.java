package com.cartethyia.easyorange.order.constant;

/**
 * 订单常量
 *
 * @author cartethyia
 * @date 2026/03/06
 */
public class OrderConstants {

    public static final String ORDER_NO_PREFIX = "ORD";

    public static final long AUTO_CONFIRM_DAYS = 7;

    // 订单号生成器常量
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
}
