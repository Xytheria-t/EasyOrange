package com.cartethyia.easyorange.payment.application.metrics;

import com.cartethyia.easyorange.payment.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMetricsListener {

    private final PaymentMetricsService metricsService;

    @Async
    @EventListener
    public void onPaymentCreated(PaymentCreatedEvent event) {
        metricsService.recordPaymentCreated();
        log.info("Payment created metrics recorded paymentId={}", event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        metricsService.recordPaymentSuccess();
        log.info("Payment success metrics recorded paymentId={}", event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        metricsService.recordPaymentFailed();
        log.info("Payment failed metrics recorded paymentId={}", event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        metricsService.recordRefund();
        log.info("Refund metrics recorded paymentId={}", event.getPaymentId());
    }
}
