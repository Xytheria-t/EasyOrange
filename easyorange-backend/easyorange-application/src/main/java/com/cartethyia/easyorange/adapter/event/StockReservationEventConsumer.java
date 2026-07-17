package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StockReservationEventConsumer {

    private final ProductCommandService productCommandService;
    private final EventIdempotencyChecker idempotencyChecker;

    @RabbitListener(
        queues = RabbitMQConfig.QUEUE_STOCK_RESERVATION,
        containerFactory = "domainEventContainerFactory"
    )
    public void onStockReservationRequested(StockReservationRequestedEvent event) {
        String eventId = "StockReservation:" + event.orderId() + ":" + event.productId();
        if (!tryAcquireLock("StockReservation", eventId)) {
            log.info("跳过重复的库存预留事件: {}", eventId);
            return;
        }

        log.info("收到库存预留请求: orderId={}, productId={}, quantity={}",
                event.orderId(), event.productId(), event.quantity());

        try {
            productCommandService.decrementStock(event.productId(), event.quantity());

            log.info("库存扣减成功: productId={}, orderId={}, quantity={}", event.productId(), event.orderId(), event.quantity());
        } catch (Exception e) {
            log.error("库存扣减失败: productId={}, orderId={}", event.productId(), event.orderId(), e);
            throw e;
        }
    }

    private boolean tryAcquireLock(String eventType, String eventId) {
        if (idempotencyChecker.isDuplicate(eventType, eventId)) {
            return false;
        }
        return idempotencyChecker.tryMark(eventType, eventId);
    }
}
