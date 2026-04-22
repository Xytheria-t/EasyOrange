package com.cartethyia.easyorange.payment.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockPaymentStrategy 测试")
class MockPaymentStrategyTest {

    private final MockPaymentStrategy strategy = new MockPaymentStrategy();

    @Test
    @DisplayName("pay 返回成功结果")
    void pay_returnsSuccessResult() {
        PaymentResult result = strategy.pay(1001L, 2001L, new BigDecimal("99.99"), 1);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isNotNull();
        assertThat(result.getTransactionId()).startsWith("TXN_");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("pay 生成唯一的交易ID")
    void pay_generatesUniqueTransactionId() throws InterruptedException {
        PaymentResult result1 = strategy.pay(1001L, 2001L, new BigDecimal("99.99"), 1);
        Thread.sleep(2);
        PaymentResult result2 = strategy.pay(1001L, 2001L, new BigDecimal("99.99"), 1);

        assertThat(result1.getTransactionId()).isNotEqualTo(result2.getTransactionId());
    }

    @Test
    @DisplayName("refund 返回成功结果")
    void refund_returnsSuccessResult() {
        RefundResult result = strategy.refund(1001L, new BigDecimal("50.00"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRefundNo()).isNotNull();
        assertThat(result.getRefundNo()).startsWith("REF_");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("refund 生成唯一的退款号")
    void refund_generatesUniqueRefundNo() throws InterruptedException {
        RefundResult result1 = strategy.refund(1001L, new BigDecimal("50.00"));
        Thread.sleep(2);
        RefundResult result2 = strategy.refund(1001L, new BigDecimal("50.00"));

        assertThat(result1.getRefundNo()).isNotEqualTo(result2.getRefundNo());
    }
}
