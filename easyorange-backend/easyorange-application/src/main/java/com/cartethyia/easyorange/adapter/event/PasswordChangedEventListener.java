package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangedEventListener {

    private final MessageCommandHandler messageCommandHandler;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.info("收到密码变更事件: userId={}", event.getUserId());

        try {
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(event.getUserId())
                    .title("密码修改安全提醒")
                    .content("您的账号密码已于 " + java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + " 成功修改。\n\n如非本人操作，请立即登录并修改密码，必要时联系客服处理。")
                    .build());

            log.info("密码变更通知发送成功: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("密码变更通知发送失败: userId={}", event.getUserId(), e);
        }
    }
}