package com.cartethyia.easyorange.payment.adapter.inbound.messaging;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_METRICS)
public class PaymentMetricsConsumer {

    private final EventConsumerHandler handler;
    private final Counter paymentCreatedCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Counter refundCounter;

    public PaymentMetricsConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService eventMetricsService,
            MeterRegistry meterRegistry) {
        this.handler =
                new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, eventMetricsService, false);
        this.paymentCreatedCounter = counter(meterRegistry, "payment.created.total", "Total number of payments created", "payment");
        this.paymentSuccessCounter = counter(meterRegistry, "payment.success.total", "Total number of successful payments", "payment");
        this.paymentFailedCounter = counter(meterRegistry, "payment.failed.total", "Total number of failed payments", "payment");
        this.refundCounter = counter(meterRegistry, "payment.refund.total", "Total number of refunds", "refund");
    }

    @RabbitHandler
    public void onPaymentCreated(PaymentCreatedEvent event, Message message) {
        handler.handle(event, message, paymentCreatedCounter::increment);
    }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceededEvent event, Message message) {
        handler.handle(event, message, paymentSuccessCounter::increment);
    }

    @RabbitHandler
    public void onPaymentFailed(PaymentFailedEvent event, Message message) {
        handler.handle(event, message, paymentFailedCounter::increment);
    }

    @RabbitHandler
    public void onPaymentRefunded(PaymentRefundedEvent event, Message message) {
        handler.handle(event, message, refundCounter::increment);
    }

    private static Counter counter(MeterRegistry registry, String name, String description, String type) {
        return Counter.builder(name).description(description).tag("type", type).register(registry);
    }
}
