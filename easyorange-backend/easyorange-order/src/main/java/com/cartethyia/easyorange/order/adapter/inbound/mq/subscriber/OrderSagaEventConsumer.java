package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_SAGA, containerFactory = "domainEventContainerFactory")
public class OrderSagaEventConsumer {

    private final EventIdempotencyChecker idempotencyChecker;
    private final DomainEventPublisher domainEventPublisher;
    private final ProductInventoryPort productInventoryPort;

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event) {
        String eventId = "OrderSagaCreated:" + event.orderId();
        if (!tryAcquireLock("OrderSagaCreated", eventId)) {
            log.info("跳过重复的 Saga 订单创建事件: {}", eventId);
            return;
        }

        log.info("收到订单创建事件: orderId={}, items count={}", event.orderId(), event.items().size());
        try {
            for (OrderCreatedEvent.OrderItemPayload item : event.items()) {
                var stockEvent = new StockReservationRequestedEvent(
                        event.orderId(), item.productId(), item.quantity());
                domainEventPublisher.publish(stockEvent);
            }
            log.info("库存预留请求已发布: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("发布库存预留请求失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        String eventId = "OrderSagaCancelled:" + event.orderId();
        if (!tryAcquireLock("OrderSagaCancelled", eventId)) {
            log.info("跳过重复的 Saga 订单取消事件: {}", eventId);
            return;
        }

        log.info("收到订单取消事件: orderId={}, productCount={}", event.orderId(), event.productIds().size());
        try {
            for (String productId : event.productIds()) {
                productInventoryPort.restoreStock(productId);
                log.info("库存恢复成功: productId={}", productId);
            }
            log.info("订单取消事件处理完成: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("订单取消事件处理失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) {
        String eventId = "OrderSagaCompleted:" + event.orderId();
        if (!tryAcquireLock("OrderSagaCompleted", eventId)) {
            log.info("跳过重复的 Saga 订单完成事件: {}", eventId);
            return;
        }

        log.info("收到订单完成事件: orderId={}, productCount={}", event.orderId(), event.productIds().size());
        try {
            for (String productId : event.productIds()) {
                productInventoryPort.markAsSold(productId);
                log.info("资产标记已售成功: productId={}", productId);
            }
            log.info("订单完成事件处理完成: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("订单完成事件处理失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event) {
        String eventId = "OrderSagaRefunded:" + event.orderId();
        if (!tryAcquireLock("OrderSagaRefunded", eventId)) {
            log.info("跳过重复的 Saga 订单退款事件: {}", eventId);
            return;
        }

        log.info("收到订单退款事件: orderId={}, productCount={}", event.orderId(), event.productIds().size());
        try {
            for (String productId : event.productIds()) {
                productInventoryPort.restoreStock(productId);
                log.info("库存恢复成功: productId={}", productId);
            }
            log.info("订单退款事件处理完成: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("订单退款事件处理失败: orderId={}", event.orderId(), e);
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
