package com.cartethyia.easyorange.payment.application.command;

import jakarta.validation.constraints.NotBlank;

public record ClosePaymentCommand(
        @NotBlank(message = "支付 ID 不能为空") String paymentId) {}
