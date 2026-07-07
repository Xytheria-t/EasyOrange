package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.PaymentInitiationRequestedEvent;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentInitiationEventConsumer {

    private final PaymentCommandHandler paymentCommandHandler;

    @RabbitListener(
        queues = RabbitMQConfig.QUEUE_PAYMENT_INITIATION,
        containerFactory = "domainEventContainerFactory"
    )
    public void onPaymentInitiationRequested(PaymentInitiationRequestedEvent event) {
        log.info("收到支付发起请求: orderId={}, amount={}", event.orderId(), event.amount());

        try {
            CreatePaymentCommand command = CreatePaymentCommand.builder()
                    .orderId(event.orderId())
                    .amount(event.amount())
                    .paymentMethod(event.paymentMethod())
                    .attach(event.attach())
                    .build();

            paymentCommandHandler.handle(command);

            log.info("支付创建成功: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("支付创建失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }
}
