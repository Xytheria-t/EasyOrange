package com.cartethyia.easyorange.payment.adapter.outbound.gateway;

import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.constant.PaymentConstant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("dev")
public class PaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public PaymentResult pay(Payment aggregate) {
        String transactionId = PaymentConstant.MOCK_TXN_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentResult.success(transactionId);
    }

    @Override
    public RefundResult refund(Payment aggregate, BigDecimal refundAmount) {
        String refundNo = PaymentConstant.MOCK_REFUND_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return RefundResult.success(refundNo);
    }
}
