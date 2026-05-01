package com.cartethyia.easyorange.payment.integration;

import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("支付 Saga 流程集成测试")
class PaymentSagaIntegrationTest {

    @Autowired
    private PaymentCommandHandler commandHandler;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("完整的支付流程应该成功")
    void completePaymentFlow_shouldSucceed() {
        CreatePaymentCommand createCommand = CreatePaymentCommand.builder()
                .orderId(System.currentTimeMillis())
                .amount(new BigDecimal("100.00"))
                .paymentMethod(1)
                .build();

        Long paymentId = commandHandler.handle(createCommand);
        
        PaymentAggregate createdAggregate = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(createdAggregate.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(createdAggregate.version()).isEqualTo(0L);

        PayCommand payCommand = PayCommand.builder()
                .paymentNo(createdAggregate.paymentNo())
                .build();
        
        commandHandler.handle(payCommand);

        PaymentAggregate paidAggregate = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(paidAggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paidAggregate.transactionId()).isNotNull();
        assertThat(paidAggregate.version()).isGreaterThan(0);
    }

    @Test
    @DisplayName("支付失败后状态应该正确更新")
    void paymentFailed_shouldUpdateStatus() {
        CreatePaymentCommand createCommand = CreatePaymentCommand.builder()
                .orderId(System.currentTimeMillis())
                .amount(new BigDecimal("100.00"))
                .paymentMethod(1)
                .build();

        Long paymentId = commandHandler.handle(createCommand);
        
        PaymentAggregate aggregate = paymentRepository.findById(paymentId).orElseThrow();
        
        assertThat(aggregate.status()).isEqualTo(PaymentStatus.PENDING);
    }
}
