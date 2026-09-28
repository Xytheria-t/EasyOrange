package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import jakarta.validation.constraints.NotNull;

public record MockPaymentRequest(
        @NotNull(message = "支付ID不能为空") String paymentId, Boolean success) {}
