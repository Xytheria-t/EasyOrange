package com.cartethyia.easyorange.payment.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentNo 值对象测试")
class PaymentNoTest {

    @Test
    @DisplayName("合法支付编号通过")
    void validPaymentNo() {
        PaymentNo paymentNo = PaymentNo.of("PAY123");

        assertThat(paymentNo.value()).isEqualTo("PAY123");
        assertThat(paymentNo.toString()).isEqualTo("PAY123");
    }

    @Test
    @DisplayName("空编号抛出异常")
    void nullPaymentNo_throws() {
        assertThatThrownBy(() -> PaymentNo.of(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("非 PAY 前缀编号抛出异常")
    void invalidPrefix_throws() {
        assertThatThrownBy(() -> PaymentNo.of("ABC123")).isInstanceOf(BusinessException.class);
    }
}
