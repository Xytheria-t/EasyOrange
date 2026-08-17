package com.cartethyia.easyorange.payment.application.command;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 支付网关回调命令 — 渠道侧支付已完成的通知，而非发起支付请求。
 * <p>
 * 与 {@link PayCommand}（「准备 → 网关 → 确认」两阶段，由我方调用网关扣款）不同，
 * 回调到来时扣款已在渠道侧完成，系统直接以回调携带的 transactionId 确认支付成功，
 * 不再二次调用网关（见 {@code PaymentCommandHandler#handle(PaymentCallbackCommand)}）。
 *
 * @param paymentNo     支付单号
 * @param transactionId 渠道交易流水号
 * @param amount        回调金额，非空时校验与支付单一致（防篡改）
 */
public record PaymentCallbackCommand(
        @NotBlank(message = "支付单号不能为空") String paymentNo,

        @NotBlank(message = "渠道交易流水号不能为空") String transactionId,

        BigDecimal amount) {}
