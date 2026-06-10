package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventConsumer {

    private final ProductInventoryPort productInventoryPort;

    @RabbitListener(
        queues = "eo.product.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("收到订单完成事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());

        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.markAsSold(productId);
                log.info("商品标记已售成功: productId={}", productId);
            }
            log.info("订单完成事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单完成事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}