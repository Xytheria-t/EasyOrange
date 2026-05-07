package com.cartethyia.easyorange.order.domain.constant;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.Getter;

/**
 * 订单模块错误码
 * <p>
 * 错误码范围：B3001-B3999
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
public enum OrderResultCode implements IResultCode {

    ORDER_NOT_FOUND("B3001", "订单不存在"),
    ORDER_STATUS_ERROR("B3002", "订单状态异常"),
    ORDER_NOT_OWNER("B3003", "非订单所有者"),
    ORDER_ALREADY_PAID("B3004", "订单已支付"),
    ORDER_ALREADY_CANCELLED("B3005", "订单已取消"),
    ORDER_ALREADY_COMPLETED("B3006", "订单已完成"),
    ORDER_CANNOT_CANCEL("B3007", "订单无法取消"),
    ORDER_CANNOT_REFUND("B3008", "订单无法退款");

    private final String code;
    private final String message;

    OrderResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
