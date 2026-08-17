package com.cartethyia.easyorange.payment.domain.constant;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付模块错误码
 * <p>
 * 错误码范围：B4001-B4999；{@link #PAYMENT_BUSY} 例外走 A0429（码内数字推导 429，
 * 表达「繁忙可重试」，与全局限流约定一致，见 {@link IResultCode#resolveStatus(String)}）。
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum PaymentResultCode implements IResultCode {
    PAYMENT_NOT_FOUND("B4001", "支付记录不存在"),
    PAYMENT_FAILED("B4002", "支付失败"),
    PAYMENT_TIMEOUT("B4003", "支付超时"),
    PAYMENT_ALREADY_REFUNDED("B4004", "已退款"),
    REFUND_NOT_FOUND("B4005", "退款记录不存在"),
    REFUND_NOT_ALLOWED("B4006", "不允许退款"),
    PAYMENT_INVALID_STATUS("B4007", "支付状态异常"),
    CALLBACK_SIGN_INVALID("B4008", "回调签名验证失败"),
    CALLBACK_AMOUNT_MISMATCH("B4011", "回调金额与支付单金额不一致"),
    PAYMENT_GATEWAY_ERROR("B4009", "支付网关调用失败"),
    SAGA_EXECUTION_FAILED("B4010", "Saga执行失败"),
    PAYMENT_BUSY("A0429", "支付处理繁忙，请稍后重试");

    private final String code;
    private final String message;
}
