package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.payment.domain.event.CompensationFailedAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 补偿失败告警消费者 — 记录 Saga 补偿失败事件的 ERROR 日志并上报指标。
 * <p>
 * 监听 {@code CompensationFailedAlertEvent}，当支付/退款 Saga 补偿操作失败时由支付模块发布。
 * 该事件此前无消费者（死代码），现通过 ERROR 日志 + DLQ 监控提供可观测性。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_COMPENSATION_ALERT, containerFactory = "domainEventContainerFactory")
public class CompensationFailedAlertConsumer {

    private final EventConsumerHandler handler;

    public CompensationFailedAlertConsumer(EventIdempotencyChecker idempotencyChecker,
                                           EventMetricsService metricsService) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
    }

    @RabbitHandler
    public void onCompensationFailed(CompensationFailedAlertEvent event, Message message) {
        handler.handle(event, message, metadata ->
                log.error("event=compensation_failed paymentId={} operationType={} errorMessage={} details={}",
                        event.paymentId(), event.operationType(), event.errorMessage(), event.failureDetails()));
    }
}
