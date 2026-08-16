package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    String createPayment(CreatePaymentRequest request);

    /**
     * 发起支付 — 由支付模块执行「准备 → 网关 → 确认」两阶段，支付成功后经
     * {@code PaymentSucceededEvent} 事件桥接回订单侧置 PAID。
     */
    void pay(String orderId);

    void refundPayment(String orderId, String reason);

    record CreatePaymentRequest(
            String orderId,
            BigDecimal amount,
            String paymentMethod,
            String attach,
            String description,
            String buyerId) {}
}
