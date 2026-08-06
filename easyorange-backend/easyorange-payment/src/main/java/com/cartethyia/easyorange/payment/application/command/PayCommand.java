package com.cartethyia.easyorange.payment.application.command;

import jakarta.validation.constraints.NotBlank;

public record PayCommand(@NotBlank(message = "支付单号不能为空") String paymentNo, String transactionId, String attach) {}
