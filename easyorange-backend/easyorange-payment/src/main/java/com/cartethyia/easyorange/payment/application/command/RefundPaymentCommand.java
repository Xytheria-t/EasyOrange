package com.cartethyia.easyorange.payment.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RefundPaymentCommand(
        @NotBlank(message = "支付 ID 不能为空") String paymentId,

        @NotNull(message = "退款金额不能为空") @Positive(message = "退款金额必须大于 0")
        BigDecimal refundAmount,

        @NotBlank(message = "退款原因不能为空") String refundReason) {}
