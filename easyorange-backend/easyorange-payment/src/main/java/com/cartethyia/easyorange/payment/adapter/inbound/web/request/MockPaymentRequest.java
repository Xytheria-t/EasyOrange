package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MockPaymentRequest(
        @NotNull(message = "支付ID不能为空") String paymentId,
        String paymentNo,
        BigDecimal amount,
        String paymentMethod,
        Boolean success) {}
