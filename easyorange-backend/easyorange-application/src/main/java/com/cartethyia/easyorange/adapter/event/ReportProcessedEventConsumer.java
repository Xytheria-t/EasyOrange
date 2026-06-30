package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportProcessedEventConsumer {

    private final MessageCommandHandler messageCommandHandler;

    @RabbitListener(
        queues = RabbitMQConfig.QUEUE_REPORT_NOTIFICATION,
        containerFactory = "domainEventContainerFactory"
    )
    public void onReportProcessed(ReportProcessedEvent event) {
        log.info("收到举报处理事件: productId={}, reporterId={}",
                event.productId(), event.reporterId());

        try {
            String title = event.approved() ? "举报处理结果：已受理" : "举报处理结果：已驳回";
            String content = buildNotificationContent(event);

            SendSystemMessageCommand command = SendSystemMessageCommand.builder()
                    .receiverId(String.valueOf(event.reporterId()))
                    .title(title)
                    .content(content)
                    .businessId(String.valueOf(event.productId()))
                    .build();

            messageCommandHandler.handle(command);

            log.info("举报结果通知发送成功: productId={}", event.productId());
        } catch (Exception e) {
            log.error("举报结果通知发送失败: productId={}", event.productId(), e);
            throw e;
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
