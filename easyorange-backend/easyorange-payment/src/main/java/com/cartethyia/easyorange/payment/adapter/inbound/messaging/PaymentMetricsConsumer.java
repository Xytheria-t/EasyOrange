package com.cartethyia.easyorange.payment.adapter.inbound.messaging;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.payment.application.metrics.PaymentMetricsService;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 支付指标消费者 — 将支付事件转为业务指标。
 * <p>
 * 关闭幂等检查：指标累加是幂等的（重复 +1 对监控数据无实质影响）。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_METRICS, containerFactory = "domainEventContainerFactory")
public class PaymentMetricsConsumer {

    private final EventConsumerHandler handler;
    private final PaymentMetricsService metricsService;

    public PaymentMetricsConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService eventMetricsService,
            PaymentMetricsService metricsService) {
        this.handler =
                new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, eventMetricsService, false);
        this.metricsService = metricsService;
    }

    @RabbitHandler
    public void onPaymentCreated(PaymentCreatedEvent event, Message message) {
        handler.handle(event, message, _ -> metricsService.recordPaymentCreated());
    }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceededEvent event, Message message) {
        handler.handle(event, message, _ -> metricsService.recordPaymentSuccess());
    }

    @RabbitHandler
    public void onPaymentFailed(PaymentFailedEvent event, Message message) {
        handler.handle(event, message, _ -> metricsService.recordPaymentFailed());
    }

    @RabbitHandler
    public void onPaymentRefunded(PaymentRefundedEvent event, Message message) {
        handler.handle(event, message, _ -> metricsService.recordRefund());
    }
}
