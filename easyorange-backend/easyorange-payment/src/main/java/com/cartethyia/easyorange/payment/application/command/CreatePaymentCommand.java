package com.cartethyia.easyorange.payment.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePaymentCommand(
        @NotBlank(message = "订单 ID 不能为空") String orderId,

        @NotNull(message = "支付金额不能为空") @Positive(message = "支付金额必须大于 0")
        BigDecimal amount,

        @NotBlank(message = "支付方式不能为空") String paymentMethod,
        String payPassword,
        String attach) {}
