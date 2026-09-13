package com.cartethyia.easyorange.order.adapter.inbound.messaging;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderItemRef;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import java.util.List;
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
        handler.handle(event, message, () -> restoreStock(event.orderId(), event.items(), "取消"));
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
            restoreStock(event.orderId(), event.items(), "退款");
            paymentGatewayPort.refundPayment(event.orderId(), event.reason());
        });
    }

    /**
     * 按事件携带的资产明细恢复库存（数量与下单扣减一致）。
     * <p>
     * 幂等由 product 模块的库存流水唯一键兜底：同一订单对同一资产重复投递只会恢复一次。
     * 明细为空说明载荷缺字段（旧格式）或已损坏，此时显性失败进重试 / DLQ 人工介入——
     * 若静默跳过，这批资产的库存会永久少回，且没有任何信号。
     */
    private void restoreStock(String orderId, List<OrderItemRef> items, String scene) {
        if (items.isEmpty()) {
            throw new IllegalStateException("订单" + scene + "事件缺少资产明细，无法恢复库存: orderId=" + orderId);
        }
        for (var item : items) {
            productInventoryPort.restoreStock(orderId, item.productId(), item.quantity());
        }
    }
}
