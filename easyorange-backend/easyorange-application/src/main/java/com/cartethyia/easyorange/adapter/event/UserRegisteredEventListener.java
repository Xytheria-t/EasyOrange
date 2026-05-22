package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final MessageCommandHandler messageCommandHandler;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("收到用户注册事件: userId={}, username={}", event.getUserId(), event.getUsername());

        try {
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(event.getUserId())
                    .title("欢迎加入 EasyOrange")
                    .content("亲爱的 %s，您好！\n\n恭喜您成功注册 EasyOrange 账号。在这里，您可以浏览和购买各类优质商品，享受便捷的购物体验。\n\n建议您尽快完善个人资料，开启愉快的购物之旅！"
                            .formatted(event.getUsername()))
                    .build());

            log.info("注册欢迎消息发送成功: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("注册欢迎消息发送失败: userId={}, username={}", event.getUserId(), event.getUsername(), e);
        }
    }
}