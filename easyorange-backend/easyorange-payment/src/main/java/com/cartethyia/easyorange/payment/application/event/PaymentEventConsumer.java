package com.cartethyia.easyorange.payment.application.event;

import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentEventConsumer {

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("event=PaymentCreated paymentId={} orderId={}", event.getPaymentId(), event.getOrderId());
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("event=PaymentSucceeded paymentId={} transactionId={}", event.getPaymentId(), event.getTransactionId());
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("event=PaymentFailed paymentId={} reason={}", event.getPaymentId(), event.getReason());
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        log.info("event=PaymentRefunded paymentId={} refundReason={}", event.getPaymentId(), event.getRefundReason());
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onPaymentClosed(PaymentClosedEvent event) {
        log.info("event=PaymentClosed paymentId={}", event.getPaymentId());
    }
}
