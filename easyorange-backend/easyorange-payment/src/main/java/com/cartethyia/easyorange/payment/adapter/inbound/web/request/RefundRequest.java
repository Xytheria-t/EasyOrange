package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RefundRequest(
        @NotNull(message = "支付 ID 不能为空") String paymentId, BigDecimal refundAmount, String refundReason) {}
