package com.cartethyia.easyorange.order.enums;

import com.cartethyia.easyorange.common.util.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    /**
     * 待付款
     */
    PENDING_PAYMENT(0, "待付款"),

    /**
     * 已付款
     */
    PAID(1, "已付款"),

    /**
     * 已发货
     */
    SHIPPED(2, "已发货"),

    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(4, "已取消"),

    /**
     * 已退款
     */
    REFUNDED(5, "已退款");

    /**
     * 状态值
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据状态值获取枚举
     */
    public static OrderStatus fromCode(Integer code) {
        return EnumUtils.fromCodeSafe(code, values(), OrderStatus::getCode).orElse(null);
    }

    /**
     * 根据状态值获取描述
     */
    public static String getDescByCode(Integer code) {
        OrderStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }

    /**
     * 判断是否可以取消（仅待付款状态可取消）
     */
    public static boolean canCancel(Integer status) {
        return PENDING_PAYMENT.getCode().equals(status);
    }

    /**
     * 判断是否可以付款（仅待付款状态可付款）
     */
    public static boolean canPay(Integer status) {
        return PENDING_PAYMENT.getCode().equals(status);
    }

    /**
     * 判断是否可以发货
     */
    public static boolean canShip(Integer status) {
        return PAID.getCode().equals(status);
    }

    /**
     * 判断是否可以确认收货
     */
    public static boolean canConfirmReceipt(Integer status) {
        return SHIPPED.getCode().equals(status);
    }

    /**
     * 判断是否可以退款
     */
    public static boolean canRefund(Integer status) {
        return PAID.getCode().equals(status) || SHIPPED.getCode().equals(status);
    }
}
