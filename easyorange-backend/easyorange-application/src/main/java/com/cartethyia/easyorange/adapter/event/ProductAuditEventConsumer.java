package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 商品审核结果消费者 — 将审核通过/驳回事件转为站内消息通知卖家。
 * <p>
 * 监听 {@code ProductAuditedEvent}，按 {@link AuditAction} 路由通知文案。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_AUDIT_NOTIFICATION)
public class ProductAuditEventConsumer {

    private final EventConsumerHandler handler;
    private final MessageCommandHandler messageCommandHandler;

    public ProductAuditEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            MessageCommandHandler messageCommandHandler) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.messageCommandHandler = messageCommandHandler;
    }

    @RabbitHandler
    public void onProductAudited(ProductAuditedEvent event, Message message) {
        handler.handle(event, message, () -> {
            var approved = event.action() == AuditAction.APPROVED;
            var title = approved ? "商品审核通过" : "商品审核未通过";
            var content = approved
                    ? "您发布的「%s」已通过审核，现已上架销售！".formatted(event.productName())
                    : "您发布的「%s」未通过审核。原因：%s。请修改后重新提交。".formatted(event.productName(), event.reason());
            messageCommandHandler.handle(
                    new SendSystemMessageCommand(event.sellerId(), title, content, event.productId()));
        });
    }
}
