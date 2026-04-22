package com.cartethyia.easyorange.payment.domain.strategy;

import com.cartethyia.easyorange.payment.constant.PaymentConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult pay(Long paymentId, Long orderId, BigDecimal amount, Integer paymentMethod) {
        String transactionId = PaymentConstants.MOCK_TXN_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentResult.success(transactionId);
    }

    @Override
    public RefundResult refund(Long paymentId, BigDecimal refundAmount) {
        String refundNo = PaymentConstants.MOCK_REFUND_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return RefundResult.success(refundNo);
    }
}
