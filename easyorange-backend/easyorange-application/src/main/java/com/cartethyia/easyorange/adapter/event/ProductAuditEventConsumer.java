package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProductAuditEventConsumer {

    private final MessageCommandHandler messageCommandHandler;

    @RabbitListener(
        queues = "eo.message.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onProductAudited(ProductAuditedEvent event) {
        log.info("收到商品审核事件: productId={}, action={}",
                event.productId(), event.action());

        try {
            if (event.action() == 1) {
                messageCommandHandler.handle(SendSystemMessageCommand.builder()
                        .receiverId(event.sellerId())
                        .title("商品审核通过")
                        .content("您发布的「%s」已通过审核，现已上架销售！".formatted(event.productName()))
                        .businessId(event.productId())
                        .build());
            } else {
                messageCommandHandler.handle(SendSystemMessageCommand.builder()
                        .receiverId(event.sellerId())
                        .title("商品审核未通过")
                        .content("您发布的「%s」未通过审核。原因：%s。请修改后重新提交。"
                                .formatted(event.productName(), event.reason()))
                        .businessId(event.productId())
                        .build());
            }

            log.info("审核通知发送成功: productId={}", event.productId());
        } catch (Exception e) {
            log.error("审核通知发送失败: productId={}", event.productId(), e);
            throw e;
        }
    }
}
