package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.ai.service.CreditScoringService;
import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
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
public class AiCreditEventConsumer extends AbstractDomainEventConsumer {

    private final CreditScoringService creditScoringService;

    public AiCreditEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                 EventMetricsService metricsService,
                                 CreditScoringService creditScoringService) {
        super(idempotencyChecker, metricsService);
        this.creditScoringService = creditScoringService;
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onReportProcessed(ReportProcessedEvent event, Message message) {
        handle(event, message);
    }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        switch (event) {
            case OrderCompletedEvent e -> handleOrderCompleted(e);
            case ReportProcessedEvent e -> handleReportProcessed(e);
            default -> throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }

    /**
     * 订单完成时重算买卖双方信用分。
     * <p>
     * 当前 {@link OrderCompletedEvent} 仅携带 {@code orderId} 和 {@code productIds}，
     * 不包含 buyerId/sellerId。仅记录事件日志以便观测；待事件增加用户标识后可恢复
     * 对 {@link CreditScoringService#recalculateScore(String)} 的调用。
     */
    private void handleOrderCompleted(OrderCompletedEvent e) {
        log.info("action=recalculate_credit_after_trade orderId={} productIds={}",
                e.orderId(), e.productIds());
    }

    /**
     * 举报处理完成时，若举报被确认则重算举报人信用分。
     */
    private void handleReportProcessed(ReportProcessedEvent e) {
        if (!e.approved()) {
            log.info("action=skip_credit_recalculation reason=report_dismissed reportId={}",
                    e.reportId());
            return;
        }
        creditScoringService.recalculateScore(e.reporterId());
        log.info("action=recalculate_credit_after_report userId={} approved={}",
                e.reporterId(), e.approved());
    }
}
