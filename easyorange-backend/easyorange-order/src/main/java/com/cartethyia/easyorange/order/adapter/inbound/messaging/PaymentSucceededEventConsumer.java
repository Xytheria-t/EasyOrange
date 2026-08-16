package com.cartethyia.easyorange.order.adapter.inbound.messaging;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 支付成功事件消费者 — 「支付成功 → 订单置 PAID」事件桥接。
 * <p>
 * 订单的 PAID 状态唯一由 {@code PaymentSucceededEvent} 驱动：payment 模块确认支付成功后
 * 经 Outbox 发布事件（routing key {@code payment.succeeded}），本消费者在订单侧走
 * {@code OrderCommandHandler.handlePaymentSucceeded} 应用状态机守卫置 PAID。
 * 事件经 {@link EventConsumerHandler} 基于 eventId 去重（重复投递 / DLQ 重投无副作用），
 * 处理失败由容器重试并最终进 DLQ/terminal 人工介入。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PAYMENT)
public class PaymentSucceededEventConsumer {

    private final EventConsumerHandler handler;
    private final OrderCommandHandler orderCommandHandler;

    public PaymentSucceededEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            OrderCommandHandler orderCommandHandler) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.orderCommandHandler = orderCommandHandler;
    }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceededEvent event, Message message) {
        handler.handle(event, message, () -> {
            if (!StringUtils.hasText(event.orderId())) {
                // 滚动部署窗口兼容：旧版消息无 orderId 字段，降级跳过并告警，不落脏数据
                log.warn("action=skip_payment_bridge reason=missing_order_id paymentId={}", event.paymentId());
                return;
            }
            orderCommandHandler.handlePaymentSucceeded(event.orderId());
        });
    }
}
