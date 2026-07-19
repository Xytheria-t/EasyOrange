package com.cartethyia.easyorange.payment.application.metrics;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
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
public class PaymentMetricsConsumer extends AbstractDomainEventConsumer {

    private final PaymentMetricsService metricsService;

    public PaymentMetricsConsumer(EventIdempotencyChecker idempotencyChecker,
                                   EventMetricsService eventMetricsService,
                                   PaymentMetricsService metricsService) {
        super(idempotencyChecker, eventMetricsService, false);
        this.metricsService = metricsService;
    }

    @RabbitHandler
    public void onPaymentCreated(PaymentCreatedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceededEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onPaymentFailed(PaymentFailedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onPaymentRefunded(PaymentRefundedEvent event, Message message) { handle(event, message); }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        switch (event) {
            case PaymentCreatedEvent ignored -> metricsService.recordPaymentCreated();
            case PaymentSucceededEvent ignored -> metricsService.recordPaymentSuccess();
            case PaymentFailedEvent ignored -> metricsService.recordPaymentFailed();
            case PaymentRefundedEvent ignored -> metricsService.recordRefund();
            default -> throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }
}
