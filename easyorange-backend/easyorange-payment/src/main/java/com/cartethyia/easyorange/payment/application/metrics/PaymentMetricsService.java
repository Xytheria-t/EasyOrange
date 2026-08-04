package com.cartethyia.easyorange.payment.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class PaymentMetricsService {

    private final Counter paymentCreatedCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Counter refundCounter;
    private final Counter concurrentPaymentConflictCounter;

    public PaymentMetricsService(MeterRegistry meterRegistry) {
        this.paymentCreatedCounter = Counter.builder("payment.created.total")
                .description("Total number of payments created")
                .tag("type", "payment")
                .register(meterRegistry);

        this.paymentSuccessCounter = Counter.builder("payment.success.total")
                .description("Total number of successful payments")
                .tag("type", "payment")
                .register(meterRegistry);

        this.paymentFailedCounter = Counter.builder("payment.failed.total")
                .description("Total number of failed payments")
                .tag("type", "payment")
                .register(meterRegistry);

        this.refundCounter = Counter.builder("payment.refund.total")
                .description("Total number of refunds")
                .tag("type", "refund")
                .register(meterRegistry);

        this.concurrentPaymentConflictCounter = Counter.builder("payment.concurrent.conflict.total")
                .description("Total number of concurrent payment conflicts")
                .tag("type", "concurrency")
                .register(meterRegistry);
    }

    public void recordPaymentCreated() {
        paymentCreatedCounter.increment();
    }

    public void recordPaymentSuccess() {
        paymentSuccessCounter.increment();
    }

    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
    }

    public void recordRefund() {
        refundCounter.increment();
    }

    public void recordConcurrentConflict() {
        concurrentPaymentConflictCounter.increment();
    }
}
