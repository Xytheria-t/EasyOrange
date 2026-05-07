package com.cartethyia.easyorange.payment.domain.constant;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.Getter;

/**
 * 支付模块错误码
 * <p>
 * 错误码范围：B4001-B4999
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
public enum PaymentResultCode implements IResultCode {

    PAYMENT_NOT_FOUND("B4001", "支付记录不存在"),
    PAYMENT_FAILED("B4002", "支付失败"),
    PAYMENT_TIMEOUT("B4003", "支付超时"),
    PAYMENT_ALREADY_REFUNDED("B4004", "已退款"),
    REFUND_NOT_FOUND("B4005", "退款记录不存在"),
    REFUND_NOT_ALLOWED("B4006", "不允许退款"),
    PAYMENT_INVALID_STATUS("B4007", "支付状态异常"),
    CALLBACK_SIGN_INVALID("B4008", "回调签名验证失败"),
    PAYMENT_GATEWAY_ERROR("B4009", "支付网关调用失败");

    private final String code;
    private final String message;

    PaymentResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
