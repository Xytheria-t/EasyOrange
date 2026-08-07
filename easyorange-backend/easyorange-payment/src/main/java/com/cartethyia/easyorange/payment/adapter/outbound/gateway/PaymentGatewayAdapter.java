package com.cartethyia.easyorange.payment.adapter.outbound.gateway;

import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentConstant;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
// dev/it 均为非生产演示角色：dev 正常演示支付，it 下的集成测试需加载完整上下文，
// mock 支付网关缺失会导致 PaymentCommandHandler 装配失败（PaymentGatewayPort 无 bean）
@Profile({"dev", "it"})
public class PaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public PaymentResult pay(Payment aggregate) {
        String transactionId = PaymentConstant.MOCK_TXN_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentResult.success(transactionId);
    }

    @Override
    public RefundResult refund(Payment aggregate, BigDecimal refundAmount) {
        String refundNo = PaymentConstant.MOCK_REFUND_NO_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return RefundResult.success(refundNo);
    }
}
