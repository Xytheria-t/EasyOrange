package com.cartethyia.easyorange.payment.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 支付命令 — 只以支付单号为键：交易号由网关返回（见 {@code PaymentResult}），命令不预设。
 */
public record PayCommand(@NotBlank(message = "支付单号不能为空") String paymentNo) {}
