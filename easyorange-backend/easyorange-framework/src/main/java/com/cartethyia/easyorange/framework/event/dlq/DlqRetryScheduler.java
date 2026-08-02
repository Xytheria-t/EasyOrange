package com.cartethyia.easyorange.framework.event.dlq;

import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DLQ 分级重试调度器 — 定时从 DLQ 拉取死信消息，按策略重投主队列或转储 terminal 队列。
 * <p>
 * 三级重试流程：
 * <ol>
 *   <li>主队列 RetryTemplate 指数退避 3 次（由 {@code domainEventContainerFactory} 配置）</li>
 *   <li>消息进入 DLQ → 本调度器每 5 分钟拉取，检查 {@code x-retry-count} 头</li>
 *   <li>重试次数 &lt; max → 重投主交换（原 routing key），{@code x-retry-count + 1}</li>
 *   <li>重试次数 ≥ max → 转储 {@code eo.dlq.terminal} 队列，等待人工介入</li>
 * </ol>
 * <p>
 * 幂等安全：重投消息保留原 body + eventId，消费者端 {@link com.cartethyia.easyorange.framework.event.core.EventConsumerHandler}
 * 幂等检查基于 eventId 去重，重复投递不会产生副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DlqRetryScheduler {

    private static final String X_RETRY_COUNT_HEADER = "x-retry-count";
    private static final String X_DEATH_HEADER = "x-death";
    private static final String X_DEATH_ROUTING_KEYS = "routing-keys";

    private static final int BATCH_SIZE = 10;
    private static final long RECEIVE_TIMEOUT_MILLIS = 1000L;

    /** 所有需要扫描的 DLQ 队列（与 RabbitMQConfig 中声明的主队列一一对应） */
    private static final List<String> DLQ_QUEUES = List.of(
            RabbitMQConfig.QUEUE_PRODUCT_CQRS + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_LIFECYCLE + ".dlq",
            RabbitMQConfig.QUEUE_AUDIT_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_AUDIT_LOG + ".dlq",
            RabbitMQConfig.QUEUE_REPORT_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET + ".dlq",
            RabbitMQConfig.QUEUE_PAYMENT_METRICS + ".dlq",
            RabbitMQConfig.QUEUE_AI_PRODUCT + ".dlq",
            RabbitMQConfig.QUEUE_AI_CREDIT + ".dlq",
            RabbitMQConfig.QUEUE_COMPENSATION_ALERT + ".dlq"
    );

    private final RabbitTemplate rabbitTemplate;
    private final DlqRetryStrategy strategy;
    private final EventMetricsService metricsService;

    /**
     * 每 5 分钟扫描所有 DLQ 队列，拉取死信消息重投或转储。
     * <p>
     * fixedDelay=300000 确保上次执行完成后等待 5 分钟再开始下一次，避免并发扫描重叠。
     */
    @Scheduled(fixedDelay = 300_000)
    public void retryFromDlq() {
        for (String dlqQueue : DLQ_QUEUES) {
            try {
                processBatch(dlqQueue);
            } catch (Exception e) {
                log.error("action=dlq_retry_scan_failed, queue={}", dlqQueue, e);
            }
        }
    }

    private void processBatch(String dlqQueue) {
        int processed = 0;
        while (processed < BATCH_SIZE) {
            Message message = rabbitTemplate.receive(dlqQueue, RECEIVE_TIMEOUT_MILLIS);
            if (message == null) {
                break;
            }
            try {
                processMessage(dlqQueue, message);
                processed++;
            } catch (Exception e) {
                log.error("action=dlq_message_process_failed, queue={}", dlqQueue, e);
                republishToDlq(dlqQueue, message);
                processed++;
            }
        }
        if (processed > 0) {
            log.info("action=dlq_retry_batch, queue={}, processed={}", dlqQueue, processed);
        }
    }

    private void processMessage(String dlqQueue, Message message) {
        String originalQueue = dlqQueue.replace(".dlq", "");
        int retryCount = getRetryCount(message);
        String routingKey = extractOriginalRoutingKey(message);

        if (routingKey == null) {
            log.warn("action=dlq_terminal_no_routing_key, queue={}, retryCount={}", originalQueue, retryCount);
            moveToTerminal(dlqQueue, message, "no-routing-key");
            metricsService.recordDlq(originalQueue, "terminal_no_routing_key");
            return;
        }

        if (strategy.shouldRetry(retryCount)) {
            log.info("action=dlq_retry, queue={}, retryCount={}, delay={}ms, routingKey={}",
                    originalQueue, retryCount, strategy.getDelayMillis(retryCount), routingKey);
            republishToMainExchange(message, routingKey, retryCount, originalQueue);
            metricsService.recordDlq(originalQueue, "retry");
        } else {
            log.warn("action=dlq_terminal_max_retries, queue={}, retryCount={}, maxRetries={}",
                    originalQueue, retryCount, strategy.getMaxRetries());
            moveToTerminal(dlqQueue, message, "max-retries");
            metricsService.recordDlq(originalQueue, "terminal_max_retries");
        }
    }

    // ───────────────────────── Republish helpers ─────────────────────────

    private void republishToMainExchange(Message message, String routingKey,
                                          int currentRetryCount, String originalQueue) {
        var props = message.getMessageProperties();
        props.setHeader(X_RETRY_COUNT_HEADER, currentRetryCount + 1);
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, routingKey, message);
    }

    private void moveToTerminal(String dlqQueue, Message message, String reason) {
        var props = message.getMessageProperties();
        props.setHeader("x-terminal-reason", reason);
        props.setHeader("x-source-dlq", dlqQueue);
        rabbitTemplate.send(RabbitMQConfig.TERMINAL_QUEUE, message);
    }

    private void republishToDlq(String dlqQueue, Message message) {
        try {
            rabbitTemplate.send(RabbitMQConfig.DLQ_EXCHANGE_NAME, dlqQueue, message);
        } catch (Exception e) {
            log.error("action=dlq_republish_failed, queue={}", dlqQueue, e);
        }
    }

    // ───────────────────────── Header extraction ─────────────────────────

    @SuppressWarnings("unchecked")
    private String extractOriginalRoutingKey(Message message) {
        var headers = message.getMessageProperties().getHeaders();
        var xDeath = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
        if (xDeath == null || xDeath.isEmpty()) {
            return null;
        }
        var firstEntry = xDeath.get(0);
        var routingKeys = (List<String>) firstEntry.get(X_DEATH_ROUTING_KEYS);
        if (routingKeys == null || routingKeys.isEmpty()) {
            return null;
        }
        return routingKeys.get(0);
    }

    private int getRetryCount(Message message) {
        var headers = message.getMessageProperties().getHeaders();
        var count = headers.get(X_RETRY_COUNT_HEADER);
        if (count instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
