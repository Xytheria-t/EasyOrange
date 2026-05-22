package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForgotPasswordEventListener {

    private final MessageCommandHandler messageCommandHandler;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onForgotPassword(ForgotPasswordEvent event) {
        log.info("收到找回密码事件: userId={}", event.getUserId());

        try {
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(event.getUserId())
                    .title("找回密码申请通知")
                    .content("您好，您的账号已于 " + java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + " 提交了找回密码申请。\n\n如非本人操作，请忽略此消息。如已成功修改密码，请使用新密码登录。")
                    .build());

            log.info("找回密码通知发送成功: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("找回密码通知发送失败: userId={}", event.getUserId(), e);
        }
    }
}