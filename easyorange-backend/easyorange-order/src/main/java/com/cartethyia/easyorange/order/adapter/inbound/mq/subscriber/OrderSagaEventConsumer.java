package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单 Saga 事件消费者 — 编排订单生命周期跨模块协作。
 * <p>
 * 职责：
 * <ul>
 *   <li>{@code OrderCreatedEvent} → 发布 {@code StockReservationRequestedEvent} 请求库存预留</li>
 *   <li>{@code OrderCancelledEvent} → 调用 {@code ProductInventoryPort.restoreStock} 恢复库存</li>
 *   <li>{@code OrderCompletedEvent} → 调用 {@code ProductInventoryPort.markAsSold} 标记售出</li>
 *   <li>{@code OrderRefundedEvent} → 调用 {@code ProductInventoryPort.restoreStock} 恢复库存</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_SAGA, containerFactory = "domainEventContainerFactory")
public class OrderSagaEventConsumer extends AbstractDomainEventConsumer {

    private final DomainEventPublisher domainEventPublisher;
    private final ProductInventoryPort productInventoryPort;

    public OrderSagaEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                   EventMetricsService metricsService,
                                   DomainEventPublisher domainEventPublisher,
                                   ProductInventoryPort productInventoryPort) {
        super(idempotencyChecker, metricsService);
        this.domainEventPublisher = domainEventPublisher;
        this.productInventoryPort = productInventoryPort;
    }

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event, Message message) { handle(event, message); }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        switch (event) {
            case OrderCreatedEvent e -> handleCreated(e);
            case OrderCancelledEvent e -> handleCancelled(e);
            case OrderCompletedEvent e -> handleCompleted(e);
            case OrderRefundedEvent e -> handleRefunded(e);
            default -> throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }

    private void handleCreated(OrderCreatedEvent event) {
        for (OrderCreatedEvent.OrderItemPayload item : event.items()) {
            domainEventPublisher.publish(new StockReservationRequestedEvent(
                    event.orderId(), item.productId(), item.quantity()));
        }
    }

    private void handleCancelled(OrderCancelledEvent event) {
        for (String productId : event.productIds()) {
            productInventoryPort.restoreStock(productId);
        }
    }

    private void handleCompleted(OrderCompletedEvent event) {
        for (String productId : event.productIds()) {
            productInventoryPort.markAsSold(productId);
        }
    }

    private void handleRefunded(OrderRefundedEvent event) {
        for (String productId : event.productIds()) {
            productInventoryPort.restoreStock(productId);
        }
    }
}
