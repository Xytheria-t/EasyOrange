package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 举报处理结果消费者 — 将举报处理结果转为站内消息通知举报者。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_REPORT_NOTIFICATION, containerFactory = "domainEventContainerFactory")
public class ReportProcessedEventConsumer {

    private final EventConsumerHandler handler;
    private final MessageCommandHandler messageCommandHandler;

    public ReportProcessedEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            MessageCommandHandler messageCommandHandler) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.messageCommandHandler = messageCommandHandler;
    }

    @RabbitHandler
    public void onReportProcessed(ReportProcessedEvent event, Message message) {
        handler.handle(event, message, () -> {
            var title = event.approved() ? "举报处理结果：已受理" : "举报处理结果：已驳回";
            var content = buildContent(event);
            messageCommandHandler.handle(
                    new SendSystemMessageCommand(event.reporterId(), title, content, event.productId()));
        });
    }

    private String buildContent(ReportProcessedEvent e) {
        var sb = new StringBuilder().append("您对商品 ID: ").append(e.productId()).append(" 的举报已处理完成。\n\n");
        sb.append(e.approved() ? "处理结果：您的举报已被采纳，相关商品已做相应处理。" : "处理结果：经审核，您的举报暂未采纳。");
        if (e.remark() != null && !e.remark().isBlank()) {
            sb.append("\n\n备注：").append(e.remark());
        }
        sb.append("\n\n感谢您的反馈，帮助我们维护良好的平台环境。");
        return sb.toString();
    }
}
