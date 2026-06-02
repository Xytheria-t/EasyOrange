package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportProcessedEventListener {

    private final MessageCommandHandler messageCommandHandler;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportProcessed(ReportProcessedEvent event) {
        try {
            String title = event.approved() ? "举报处理结果：已受理" : "举报处理结果：已驳回";
            String content = buildNotificationContent(event);

            SendSystemMessageCommand command = SendSystemMessageCommand.builder()
                    .receiverId(event.reporterId())
                    .title(title)
                    .content(content)
                    .businessId(event.productId())
                    .build();

            messageCommandHandler.handle(command);
            log.info("action=report_notification_sent reportId={} reporterId={} approved={}",
                    event.reportId(), event.reporterId(), event.approved());
        } catch (Exception e) {
            log.error("action=send_report_notification_failed reportId={} error={}",
                    event.reportId(), e.getMessage(), e);
        }
    }

    private String buildNotificationContent(ReportProcessedEvent event) {
        var content = new StringBuilder();
        content.append("您对商品 ID: ").append(event.productId()).append(" 的举报已处理完成。\n\n");

        if (event.approved()) {
            content.append("处理结果：您的举报已被采纳，相关商品已做相应处理。");
        } else {
            content.append("处理结果：经审核，您的举报暂未采纳。");
        }

        if (event.remark() != null && !event.remark().isBlank()) {
            content.append("\n\n备注：").append(event.remark());
        }

        content.append("\n\n感谢您的反馈，帮助我们维护良好的平台环境。");
        return content.toString();
    }
}
