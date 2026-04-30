package com.cartethyia.easyorange.payment.adapter.outbound.gateway;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentGateway;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentResult;
import com.cartethyia.easyorange.payment.domain.gateway.RefundResult;
import com.cartethyia.easyorange.payment.constant.PaymentConstant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("dev")
public class PaymentGatewayAdapter implements PaymentGateway {

    @Override
    public PaymentResult pay(PaymentAggregate aggregate) {
        String transactionId = PaymentConstant.MOCK_TXN_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentResult.success(transactionId);
    }

    @Override
    public RefundResult refund(PaymentAggregate aggregate, BigDecimal refundAmount) {
        String refundNo = PaymentConstant.MOCK_REFUND_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return RefundResult.success(refundNo);
    }
}
