package com.cartethyia.easyorange.payment.domain.strategy;

import com.cartethyia.easyorange.payment.constant.PaymentConstant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult pay(Long paymentId, Long orderId, BigDecimal amount, Integer paymentMethod) {
        String transactionId = PaymentConstant.MOCK_TXN_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentResult.success(transactionId);
    }

    @Override
    public RefundResult refund(Long paymentId, BigDecimal refundAmount) {
        String refundNo = PaymentConstant.MOCK_REFUND_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return RefundResult.success(refundNo);
    }
}
