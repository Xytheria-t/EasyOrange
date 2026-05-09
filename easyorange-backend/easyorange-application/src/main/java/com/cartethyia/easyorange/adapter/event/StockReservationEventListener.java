package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockReservationEventListener {

    private final ProductCommandService productCommandService;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockReservationRequested(StockReservationRequestedEvent event) {
        log.info("收到库存预留请求: orderId={}, productId={}, quantity={}", 
                event.getOrderId(), event.getProductId(), event.getQuantity());
        
        try {
            int quantity = event.getQuantity();
            productCommandService.decrementStock(new DecrementStockCommand(event.getProductId(), quantity));
            
            log.info("库存扣减成功: productId={}, orderId={}, quantity={}", event.getProductId(), event.getOrderId(), quantity);
        } catch (Exception e) {
            log.error("库存扣减失败: productId={}, orderId={}", event.getProductId(), event.getOrderId(), e);
        }
    }
}
