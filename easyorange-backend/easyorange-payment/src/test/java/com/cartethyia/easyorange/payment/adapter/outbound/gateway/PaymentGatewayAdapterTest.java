package com.cartethyia.easyorange.payment.adapter.outbound.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentGatewayAdapter 测试")
class PaymentGatewayAdapterTest {

    private final PaymentGatewayAdapter adapter = new PaymentGatewayAdapter();

    private Payment aggregate() {
        var spec = new PaymentReconstructSpec(
                "1001",
                "PAY123",
                "2001",
                "3001",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                PaymentMethod.WECHAT,
                PaymentStatus.PAYING,
                null,
                null,
                null,
                null,
                null,
                null,
                0);
        return Payment.from(spec);
    }

    @Test
    @DisplayName("pay 返回成功交易号")
    void pay_returnsSuccessTransaction() {
        PaymentResult result = adapter.pay(aggregate());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).startsWith("TXN_");
    }

    @Test
    @DisplayName("refund 返回成功退款号")
    void refund_returnsSuccessRefund() {
        RefundResult result = adapter.refund(aggregate(), new BigDecimal("100.00"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRefundNo()).startsWith("REF_");
    }
}
