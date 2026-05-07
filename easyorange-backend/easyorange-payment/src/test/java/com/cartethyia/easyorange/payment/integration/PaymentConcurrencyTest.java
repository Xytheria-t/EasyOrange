package com.cartethyia.easyorange.payment.integration;

import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentRepositoryPort;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.cartethyia.easyorange.payment.PaymentTestApplication.class)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("支付并发测试")
class PaymentConcurrencyTest {

    @Autowired
    private PaymentCommandHandler commandHandler;

    @Autowired
    private PaymentRepositoryPort paymentRepository;

    @Test
    @DisplayName("并发支付应该只有一个成功（乐观锁测试）")
    void concurrentPay_shouldOnlyOneSucceed() throws InterruptedException {
        CreatePaymentCommand createCommand = CreatePaymentCommand.builder()
                .orderId(System.currentTimeMillis())
                .amount(new BigDecimal("100.00"))
                .paymentMethod(1)
                .build();

        Long paymentId = commandHandler.handle(createCommand);
        PaymentAggregate aggregate = paymentRepository.findById(paymentId).orElseThrow();
        String paymentNo = aggregate.paymentNo();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    PayCommand payCommand = PayCommand.builder()
                            .paymentNo(paymentNo)
                            .build();
                    
                    commandHandler.handle(payCommand);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        PaymentAggregate finalAggregate = paymentRepository.findById(paymentId).orElseThrow();
        
        assertThat(finalAggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
        
        System.out.println("成功次数: " + successCount.get() + ", 失败次数: " + failureCount.get());
    }
}
