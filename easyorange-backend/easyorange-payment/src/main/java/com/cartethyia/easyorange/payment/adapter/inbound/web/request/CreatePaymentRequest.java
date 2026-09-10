package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "订单 ID 不能为空") String orderId,

        @NotNull(message = "支付金额不能为空") @DecimalMin(value = "0.01", message = "支付金额必须大于0")
        BigDecimal amount,

        @NotNull(message = "支付方式不能为空") String paymentMethod) {}
