package com.cartethyia.easyorange.framework.event.dlq;

import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 死信队列异常监听器 — 监听所有 DLQ 队列，记录 metrics 与结构化 ERROR 日志。
 * <p>
 * RabbitMQ 死信路由：业务队列的 {@code x-dead-letter-exchange=eo.dlq}，
 * 消息重试耗尽或被拒绝时进入对应 {@code <queue>.dlq} 队列。
 * <p>
 * 监听方式：单个 @RabbitListener 同时监听 12 个 DLQ 队列，
 * 通过 message properties 的 {@code x-death} header 提取：
 * <ul>
 *   <li>原始 queue / exchange / routing-key</li>
 *   <li>死信原因（{@code rejected} / {@code expired} / {@code maxlen}）</li>
 *   <li>死亡时间与原 message-id</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DlqAnomalyListener {

    private static final String X_DEATH_HEADER = "x-death";
    private static final String X_FIRST_DEATH_EXCHANGE = "x-first-death-exchange";
    private static final String X_FIRST_DEATH_QUEUE = "x-first-death-queue";
    private static final String X_FIRST_DEATH_REASON = "x-first-death-reason";

    private final EventMetricsService metricsService;

    @RabbitListener(
            queues = {
                    RabbitMQConfig.QUEUE_PRODUCT_CQRS + ".dlq",
                    RabbitMQConfig.QUEUE_ORDER_NOTIFICATION + ".dlq",
                    RabbitMQConfig.QUEUE_ORDER_SAGA + ".dlq",
                    RabbitMQConfig.QUEUE_AUDIT_NOTIFICATION + ".dlq",
                    RabbitMQConfig.QUEUE_AUDIT_LOG + ".dlq",
                    RabbitMQConfig.QUEUE_REPORT_NOTIFICATION + ".dlq",
                    RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET + ".dlq",
                    RabbitMQConfig.QUEUE_PAYMENT_METRICS + ".dlq",
                    RabbitMQConfig.QUEUE_AI_PRODUCT + ".dlq",
                    RabbitMQConfig.QUEUE_AI_CREDIT + ".dlq",
                    RabbitMQConfig.QUEUE_COMPENSATION_ALERT + ".dlq"
            },
            containerFactory = "domainEventContainerFactory"
    )
    public void onDeadLetter(Message message) {
        var props = message.getMessageProperties();
        var headers = props.getHeaders();
        var queue = props.getConsumerQueue() != null
                ? props.getConsumerQueue()
                : (String) headers.get(X_FIRST_DEATH_QUEUE);
        var reason = extractReason(headers);
        var originalRoutingKey = extractFirstDeathRoutingKey(headers);

        metricsService.recordDlq(queue, reason);

        log.error("事件死信告警 dlq={} reason={} originalRoutingKey={} messageId={} bodySize={} xDeath={}",
                queue,
                reason,
                originalRoutingKey,
                props.getMessageId(),
                message.getBody() != null ? message.getBody().length : 0,
                extractXDeathSummary(headers));
    }

    @SuppressWarnings("unchecked")
    private String extractReason(Map<String, Object> headers) {
        if (headers == null) {
            return "unknown";
        }
        var firstReason = (String) headers.get(X_FIRST_DEATH_REASON);
        if (firstReason != null) {
            return firstReason;
        }
        var xDeath = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
        if (xDeath != null && !xDeath.isEmpty()) {
            var reason = xDeath.get(0).get("reason");
            return reason != null ? reason.toString() : "unknown";
        }
        return "unknown";
    }

    private String extractFirstDeathRoutingKey(Map<String, Object> headers) {
        if (headers == null) {
            return null;
        }
        var exchange = (String) headers.get(X_FIRST_DEATH_EXCHANGE);
        var queue = (String) headers.get(X_FIRST_DEATH_QUEUE);
        return "exchange=" + exchange + ",queue=" + queue;
    }

    @SuppressWarnings("unchecked")
    private String extractXDeathSummary(Map<String, Object> headers) {
        if (headers == null) {
            return "[]";
        }
        var xDeath = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
        if (xDeath == null || xDeath.isEmpty()) {
            return "[]";
        }
        var first = xDeath.get(0);
        return "queue=" + first.get("queue")
                + ",reason=" + first.get("reason")
                + ",count=" + first.get("count");
    }
}
