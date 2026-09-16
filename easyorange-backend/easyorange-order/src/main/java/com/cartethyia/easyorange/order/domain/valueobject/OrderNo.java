package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record OrderNo(String value) {

    /** 订单号前缀 — 订单号由订单 ID 派生，前缀只在此定义。 */
    private static final String PREFIX = "ORD";

    public OrderNo {
        BizRequire.notNull(value, "订单编号不能为空");
        BizRequire.requireTrue(value.startsWith(PREFIX), "订单编号格式不正确");
    }

    /**
     * 由订单 ID 派生订单号 —— 订单号的唯一生成入口：聚合创建与事件消费共用同一规则，禁止各处自行拼前缀。
     *
     * @param orderId 订单 ID（主键）
     */
    public static OrderNo forOrderId(String orderId) {
        BizRequire.notBlank(orderId, "订单 ID 不能为空");
        return new OrderNo(PREFIX + orderId);
    }

    /** 从持久化值重建。 */
    public static OrderNo of(String value) {
        return new OrderNo(value);
    }
}
