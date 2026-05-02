package com.cartethyia.easyorange.payment.application.metrics;

import com.cartethyia.easyorange.payment.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMetricsListener {

    private final PaymentMetricsService metricsService;

    @Async
    @EventListener
    public void onPaymentCreated(PaymentCreatedEvent event) {
        metricsService.recordPaymentCreated();
    }

    @Async
    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        metricsService.recordPaymentSuccess();
    }

    @Async
    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        metricsService.recordPaymentFailed();
    }

    @Async
    @EventListener
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        metricsService.recordRefund();
    }
}
