package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 库存跨模块消费者 — 订阅 Order 模块发出的库存预留请求。
 * <p>
 * 单一消费者处理库存预留事件：
 * <ul>
 *   <li>{@code StockReservationRequestedEvent} → {@code ProductCommandService.decrementStock}</li>
 * </ul>
 * 通过类级 {@code @RabbitListener} 监听队列，方法级 {@code @RabbitHandler} 按事件类型分发。
 * 幂等、metrics、日志、异常包装由 {@link AbstractDomainEventConsumer#handle} 统一处理。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(
        queues = {RabbitMQConfig.QUEUE_STOCK_RESERVATION},
        containerFactory = "domainEventContainerFactory"
)
public class OrderFulfillmentEventConsumer extends AbstractDomainEventConsumer {

    private final ProductCommandService productCommandService;

    public OrderFulfillmentEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                         EventMetricsService metricsService,
                                         ProductCommandService productCommandService) {
        super(idempotencyChecker, metricsService);
        this.productCommandService = productCommandService;
    }

    @RabbitHandler
    public void onStockReservationRequested(StockReservationRequestedEvent event, Message message) {
        handle(event, message);
    }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        if (event instanceof StockReservationRequestedEvent e) {
            handleStockReservation(e);
        } else {
            throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }

    private void handleStockReservation(StockReservationRequestedEvent event) {
        productCommandService.decrementStock(event.productId(), event.quantity());
    }
}
