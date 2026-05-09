package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.order.domain.event.PaymentInitiationRequestedEvent;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInitiationEventListener {

    private final PaymentCommandHandler paymentCommandHandler;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentInitiationRequested(PaymentInitiationRequestedEvent event) {
        log.info("收到支付发起请求: orderId={}, amount={}", event.getOrderId(), event.getAmount());
        
        try {
            CreatePaymentCommand command = CreatePaymentCommand.builder()
                    .orderId(event.getOrderId())
                    .amount(event.getAmount())
                    .paymentMethod(event.getPaymentMethod())
                    .attach(event.getAttach())
                    .build();
            
            paymentCommandHandler.handle(command);
            
            log.info("支付创建成功: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("支付创建失败: orderId={}", event.getOrderId(), e);
        }
    }
}
