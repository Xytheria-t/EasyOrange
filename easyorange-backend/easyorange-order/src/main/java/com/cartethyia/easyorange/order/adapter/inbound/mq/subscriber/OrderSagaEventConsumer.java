package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_SAGA, containerFactory = "domainEventContainerFactory")
public class OrderSagaEventConsumer {

    private final DomainEventPublisher domainEventPublisher;
    private final ProductInventoryPort productInventoryPort;

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件: orderId={}, items count={}", event.getOrderId(), event.getItems().size());
        try {
            for (OrderCreatedEvent.OrderItemPayload item : event.getItems()) {
                var stockEvent = new StockReservationRequestedEvent(
                        event.getOrderId(), item.productId(), item.quantity());
                domainEventPublisher.publish(stockEvent);
            }
            log.info("库存预留请求已发布: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("发布库存预留请求失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("收到订单取消事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());
        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.restoreStock(productId);
                log.info("库存恢复成功: productId={}", productId);
            }
            log.info("订单取消事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单取消事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("收到订单完成事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());
        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.markAsSold(productId);
                log.info("资产标记已售成功: productId={}", productId);
            }
            log.info("订单完成事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单完成事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event) {
        log.info("收到订单退款事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());
        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.restoreStock(productId);
                log.info("库存恢复成功: productId={}", productId);
            }
            log.info("订单退款事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单退款事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
