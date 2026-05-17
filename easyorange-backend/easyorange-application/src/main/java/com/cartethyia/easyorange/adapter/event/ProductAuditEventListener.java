package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAuditEventListener {

    private final MessageCommandHandler messageCommandHandler;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductAudited(ProductAuditedEvent event) {
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
            log.info("action=audit_notification_sent productId={} action={} sellerId={}",
                    event.productId(), event.action(), event.sellerId());
        } catch (Exception e) {
            log.error("action=audit_notify_failed productId={} error={}", event.productId(), e.getMessage(), e);
        }
    }
}
