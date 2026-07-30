package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.ai.service.CreditScoringService;
import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI 信用分事件消费者 — 响应式重算用户信用分。
 * <p>
 * 监听交易完成和举报处理事件，触发 {@link CreditScoringService#recalculateScore(String)}，
 * 实现跨模块事件驱动的信用分自动重算（order/product → credit score）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_AI_CREDIT, containerFactory = "domainEventContainerFactory")
public class AiCreditEventConsumer {

    private final EventConsumerHandler handler;
    private final CreditScoringService creditScoringService;

    public AiCreditEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                  EventMetricsService metricsService,
                                  CreditScoringService creditScoringService) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.creditScoringService = creditScoringService;
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event, Message message) {
        handler.handle(event, message, metadata ->
                log.info("action=recalculate_credit_after_trade orderId={} productIds={}",
                        event.orderId(), event.productIds()));
    }

    @RabbitHandler
    public void onReportProcessed(ReportProcessedEvent event, Message message) {
        handler.handle(event, message, metadata -> {
            if (!event.approved()) {
                log.info("action=skip_credit_recalculation reason=report_dismissed reportId={}", event.reportId());
                return;
            }
            creditScoringService.recalculateScore(event.reporterId());
            log.info("action=recalculate_credit_after_report userId={} approved={}",
                    event.reporterId(), event.approved());
        });
    }
}
