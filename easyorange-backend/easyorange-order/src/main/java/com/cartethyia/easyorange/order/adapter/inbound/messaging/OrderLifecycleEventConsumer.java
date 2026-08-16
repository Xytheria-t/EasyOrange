package com.cartethyia.easyorange.order.adapter.inbound.messaging;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单生命周期事件消费者 — 订单状态变更后的跨模块协作。
 * <p>
 * 注意：下单时的库存扣减已在 {@code OrderCommandHandler} 中同步完成（同事务），
 * {@code OrderCreatedEvent} 不再触发异步库存预留。
 * <p>
 * 职责：
 * <ul>
 *   <li>{@code OrderCancelledEvent} → 调用 {@code ProductInventoryPort.restoreStock} 恢复库存</li>
 *   <li>{@code OrderCompletedEvent} → 调用 {@code ProductInventoryPort.markAsSold} 标记售出</li>
 *   <li>{@code OrderRefundedEvent} → 调用 {@code ProductInventoryPort.restoreStock} 恢复库存 + {@code PaymentGatewayPort.refundPayment} 触发支付退款</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_LIFECYCLE)
public class OrderLifecycleEventConsumer {

    private final EventConsumerHandler handler;
    private final ProductInventoryPort productInventoryPort;
    private final PaymentGatewayPort paymentGatewayPort;

    public OrderLifecycleEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            ProductInventoryPort productInventoryPort,
            PaymentGatewayPort paymentGatewayPort) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.productInventoryPort = productInventoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event, Message message) {
        handler.handle(
                event,
                message,
                () -> log.debug("Order created, stock already decremented synchronously: orderId={}", event.orderId()));
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event, Message message) {
        handler.handle(event, message, () -> {
            for (var productId : event.productIds()) {
                productInventoryPort.restoreStock(productId);
            }
        });
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event, Message message) {
        handler.handle(event, message, () -> {
            for (var productId : event.productIds()) {
                productInventoryPort.markAsSold(productId);
            }
        });
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event, Message message) {
        handler.handle(event, message, () -> {
            for (var productId : event.productIds()) {
                productInventoryPort.restoreStock(productId);
            }
            paymentGatewayPort.refundPayment(event.orderId(), event.reason());
        });
    }
}
