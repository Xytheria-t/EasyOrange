package com.cartethyia.easyorange.payment.application.metrics;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_METRICS, containerFactory = "domainEventContainerFactory")
public class PaymentMetricsConsumer {

    private final PaymentMetricsService metricsService;

    @RabbitHandler
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.debug("Recording payment created metric: paymentId={}", event.paymentId());
        metricsService.recordPaymentCreated();
    }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.debug("Recording payment success metric: paymentId={}", event.paymentId());
        metricsService.recordPaymentSuccess();
    }

    @RabbitHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.debug("Recording payment failed metric: paymentId={}", event.paymentId());
        metricsService.recordPaymentFailed();
    }

    @RabbitHandler
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        log.debug("Recording refund metric: paymentId={}", event.paymentId());
        metricsService.recordRefund();
    }
}
