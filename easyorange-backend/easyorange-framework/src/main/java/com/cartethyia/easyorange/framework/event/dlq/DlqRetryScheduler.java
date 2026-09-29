package com.cartethyia.easyorange.framework.event.dlq;

import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.rabbitmq.client.GetResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DLQ 分级重试调度器 — 定时从 DLQ 拉取死信，按退避重投主队列或转储 terminal 队列。
 * <p>
 * 阶梯：容器内快速重试 2 次（退避 1s 起、10s 封顶）→ 进 DLQ 后本调度器每 5 分钟扫描一次，按
 * {@code x-retry-count} 退避（见 {@link #BACKOFF_DELAYS_MINUTES}）重投原 routing key 并 {@code +1}，累计 ≥3 次转储
 * {@code eo.dlq.terminal} 等人工介入。退避未到期的以 {@code basicNack(requeue=true)} 留队，实际重投间隔按 5 分钟扫描粒度向上取整。
 * <p>
 * 手动 ack（{@code basicGet(autoAck=false)} + 处理成功后 {@code basicAck}）：处理中途崩溃时消息随连接关闭自动回队，
 * 杜绝「receive 后、重投前」的丢失窗口；重投保留原 body + eventId，消费端
 * {@link com.cartethyia.easyorange.framework.event.core.EventConsumerHandler} 按 eventId 去重，重复投递无副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DlqRetryScheduler {

    private static final String X_RETRY_COUNT_HEADER = "x-retry-count";
    private static final String X_DEATH_HEADER = "x-death";
    private static final String X_DEATH_ROUTING_KEYS = "routing-keys";

    /** 退避时间窗（分钟）：x-retry-count=0/1/2 分别等待 1/5/15 分钟（自本次死信时间起算） */
    private static final long[] BACKOFF_DELAYS_MINUTES = {1, 5, 15};

    /** 单队列单次扫描最多拉取条数（包内可见供测试断言批次上限） */
    static final int BATCH_SIZE = 10;

    private static final int MAX_RETRIES = 3;

    /** 所有需要扫描的 DLQ 队列（与 RabbitMQConfig 中声明的主队列一一对应） */
    private static final List<String> DLQ_QUEUES = List.of(
            RabbitMQConfig.QUEUE_PRODUCT_CQRS + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_LIFECYCLE + ".dlq",
            RabbitMQConfig.QUEUE_ORDER_PAYMENT + ".dlq",
            RabbitMQConfig.QUEUE_AUDIT_NOTIFICATION + ".dlq",
            RabbitMQConfig.QUEUE_AUDIT_LOG + ".dlq",
            RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET + ".dlq",
            RabbitMQConfig.QUEUE_PAYMENT_METRICS + ".dlq");

    private final RabbitTemplate rabbitTemplate;
    private final EventMetricsService metricsService;

    /** 把 basicGet 返回的 AMQP 属性还原为 Spring MessageProperties（与 RabbitTemplate 默认转换器一致） */
    private static final MessagePropertiesConverter PROPERTIES_CONVERTER = new DefaultMessagePropertiesConverter();

    /**
     * fixedDelay=300000：上次执行完成后再等 5 分钟，避免两轮扫描重叠。
     */
    @Scheduled(fixedDelay = 300_000)
    public void retryFromDlq() {
        for (String dlqQueue : DLQ_QUEUES) {
            try {
                int processed = processQueue(dlqQueue);
                if (processed > 0) {
                    log.info("action=dlq_retry_batch, queue={}, processed={}", dlqQueue, processed);
                }
            } catch (Exception e) {
                log.error("action=dlq_retry_scan_failed, queue={}", dlqQueue, e);
            }
        }
    }

    private int processQueue(String dlqQueue) throws Exception {
        return rabbitTemplate.execute(channel -> {
            int processed = 0;
            while (processed < BATCH_SIZE) {
                GetResponse response = channel.basicGet(dlqQueue, false);
                if (response == null) {
                    break;
                }
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                Message message = toSpringMessage(response);
                try {
                    if (processMessage(dlqQueue, message) == RetryAction.WAIT) {
                        channel.basicNack(deliveryTag, false, true);
                    } else {
                        channel.basicAck(deliveryTag, false);
                    }
                } catch (Exception e) {
                    log.error("action=dlq_message_process_failed, queue={}", dlqQueue, e);
                    // 处理失败回队：手动 ack 语义下无丢失窗口
                    channel.basicNack(deliveryTag, false, true);
                }
                processed++;
            }
            return processed;
        });
    }

    /** {@code basicGet} 返回的 AMQP 属性须经标准转换器还原成 Spring {@link Message}，x-death 等头才可读。 */
    private Message toSpringMessage(GetResponse response) {
        return new Message(
                response.getBody(),
                PROPERTIES_CONVERTER.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8"));
    }

    private enum RetryAction {
        REPUBLISH,
        TERMINAL,
        WAIT
    }

    private RetryAction processMessage(String dlqQueue, Message message) {
        String originalQueue = dlqQueue.replace(".dlq", "");
        int retryCount = getRetryCount(message);
        String routingKey = extractOriginalRoutingKey(message);

        if (routingKey == null) {
            log.warn("action=dlq_terminal_no_routing_key, queue={}, retryCount={}", originalQueue, retryCount);
            moveToTerminal(dlqQueue, message, "no-routing-key");
            metricsService.recordDlq(originalQueue, "terminal_no_routing_key");
            return RetryAction.TERMINAL;
        }

        if (retryCount >= MAX_RETRIES) {
            log.warn(
                    "action=dlq_terminal_max_retries, queue={}, retryCount={}, maxRetries={}",
                    originalQueue,
                    retryCount,
                    MAX_RETRIES);
            moveToTerminal(dlqQueue, message, "max-retries");
            metricsService.recordDlq(originalQueue, "terminal_max_retries");
            return RetryAction.TERMINAL;
        }

        if (!isBackoffDue(message, retryCount)) {
            log.info("action=dlq_backoff_wait, queue={}, retryCount={}", originalQueue, retryCount);
            metricsService.recordDlq(originalQueue, "backoff_wait");
            return RetryAction.WAIT;
        }

        log.info("action=dlq_retry, queue={}, retryCount={}, routingKey={}", originalQueue, retryCount, routingKey);
        republishToMainExchange(message, routingKey, retryCount);
        metricsService.recordDlq(originalQueue, "retry");
        return RetryAction.REPUBLISH;
    }

    /**
     * 退避到期判定：自本次死信时间（{@code x-death} 首条记录的 time）起等
     * {@code BACKOFF_DELAYS_MINUTES[retryCount]} 分钟；无死信时间可参考（手工投递等）时不额外等待，直接放行。
     */
    private boolean isBackoffDue(Message message, int retryCount) {
        var headers = message.getMessageProperties().getHeaders();
        var xDeath = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
        if (xDeath == null || xDeath.isEmpty()) {
            return true;
        }
        Object time = xDeath.get(0).get("time");
        long deathMillis =
                switch (time) {
                    case null -> -1;
                    case Number n -> n.longValue();
                    case Date d -> d.getTime();
                    default -> -1;
                };
        if (deathMillis < 0) {
            return true;
        }
        long delayMillis = TimeUnit.MINUTES.toMillis(BACKOFF_DELAYS_MINUTES[retryCount]);
        return System.currentTimeMillis() >= deathMillis + delayMillis;
    }

    // ── 重投辅助方法 ──

    private void republishToMainExchange(Message message, String routingKey, int currentRetryCount) {
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

    // ── Header 提取 ──

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
