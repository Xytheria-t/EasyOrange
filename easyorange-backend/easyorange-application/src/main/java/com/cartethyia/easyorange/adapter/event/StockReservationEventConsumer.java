package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
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

    @RabbitListener(
        queues = "eo.product.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onStockReservationRequested(StockReservationRequestedEvent event) {
        log.info("收到库存预留请求: orderId={}, productId={}, quantity={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        try {
            int quantity = event.getQuantity();
            productCommandService.decrementStock(new DecrementStockCommand(event.getProductId(), quantity));

            log.info("库存扣减成功: productId={}, orderId={}, quantity={}", event.getProductId(), event.getOrderId(), quantity);
        } catch (Exception e) {
            log.error("库存扣减失败: productId={}, orderId={}", event.getProductId(), event.getOrderId(), e);
            throw e;
        }
    }
}
