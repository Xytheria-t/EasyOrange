package com.cartethyia.easyorange.order.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举 — code 为有意义字符串，DB 列 VARCHAR(20)，经 {@code @EnumValue} 持久化。
 * <p>
 * 本枚举只声明状态本身，不含任何转换判定：合法转换一律问 {@link OrderAction#canApply}，
 * 避免「只看状态可达」与「状态 + 支付维度同时满足」两份判定漂移。
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum OrderStatus implements BaseCodeEnum {

    // 按生命周期顺序声明：待付款 → 已付款 → 已发货 → 已完成（终端）
    PENDING_PAYMENT("PENDING_PAYMENT", "待付款"),
    PAID("PAID", "已付款"),
    SHIPPED("SHIPPED", "已发货"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    REFUNDED("REFUNDED", "已退款");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static OrderStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(OrderStatus.class, code);
    }
}
